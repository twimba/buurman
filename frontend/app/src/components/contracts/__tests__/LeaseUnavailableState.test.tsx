import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LeaseUnavailableState } from '../LeaseUnavailableState';

describe('LeaseUnavailableState', () => {
  it('renders the country variant with a labelled country row', async () => {
    const onAction = vi.fn();
    render(
      <LeaseUnavailableState
        reason="country"
        countryName="Italy"
        onAction={onAction}
      />
    );
    expect(
      screen.getByRole('heading', { name: 'Not available here yet' })
    ).toBeInTheDocument();
    expect(screen.getByText('Country')).toBeInTheDocument();
    expect(screen.getByText('Italy')).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole('button', { name: 'Go to Documents' })
    );
    expect(onAction).toHaveBeenCalledTimes(1);
  });

  it('omits the country row when the name is unknown', () => {
    render(<LeaseUnavailableState reason="country" onAction={vi.fn()} />);
    expect(screen.queryByText('Country')).toBeNull();
  });

  it('renders the no-country variant', () => {
    render(<LeaseUnavailableState reason="no-country" onAction={vi.fn()} />);
    expect(
      screen.getByRole('heading', { name: 'Choose a country first' })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Edit property' })
    ).toBeInTheDocument();
  });

  it('hides the action button when there is no action', () => {
    render(<LeaseUnavailableState reason="no-country" />);
    expect(screen.queryByRole('button')).toBeNull();
  });
});
