import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LeaseRegimeSelect } from '../LeaseRegimeSelect';
import { AVAILABLE_LEASE_REGIMES } from '../leaseRegimes';
import { renderWithProviders } from '@/test/test-utils';

describe('LeaseRegimeSelect', () => {
  it('is not rendered with the Slice 0 default (only STANDARD available)', () => {
    expect(AVAILABLE_LEASE_REGIMES).toEqual(['STANDARD']);
    renderWithProviders(
      <LeaseRegimeSelect value="STANDARD" onChange={vi.fn()} />
    );
    expect(screen.queryByRole('combobox')).toBeNull();
  });

  it('is rendered when more than one regime is available', async () => {
    const onChange = vi.fn();
    renderWithProviders(
      <LeaseRegimeSelect
        value="STANDARD"
        onChange={onChange}
        availableRegimes={['STANDARD', 'SHORT_TERM']}
      />
    );
    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('STANDARD');
    expect(screen.getAllByRole('option')).toHaveLength(2);
    await userEvent.selectOptions(select, 'SHORT_TERM');
    expect(onChange).toHaveBeenCalledWith('SHORT_TERM');
  });
});
