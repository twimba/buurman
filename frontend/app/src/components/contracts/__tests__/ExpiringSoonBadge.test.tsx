import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ExpiringSoonBadge } from '../ExpiringSoonBadge';

describe('ExpiringSoonBadge', () => {
  it('renders for an ACTIVE contract ending within the threshold', () => {
    const soon = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString();
    render(<ExpiringSoonBadge effectiveEndDate={soon} status="ACTIVE" />);
    expect(screen.getByText('Ending in 10 days')).toBeInTheDocument();
  });

  it('renders nothing for a non-ACTIVE contract even if ending soon', () => {
    const soon = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString();
    const { container } = render(
      <ExpiringSoonBadge effectiveEndDate={soon} status="EXPIRED" />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing for an ACTIVE contract ending beyond the threshold', () => {
    const far = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString();
    const { container } = render(
      <ExpiringSoonBadge effectiveEndDate={far} status="ACTIVE" />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('recomputes the day count when effectiveEndDate changes on an already-mounted instance', () => {
    const in10Days = new Date(
      Date.now() + 10 * 24 * 60 * 60 * 1000
    ).toISOString();
    const in50Days = new Date(
      Date.now() + 50 * 24 * 60 * 60 * 1000
    ).toISOString();

    const { rerender } = render(
      <ExpiringSoonBadge effectiveEndDate={in10Days} status="ACTIVE" />
    );
    expect(screen.getByText('Ending in 10 days')).toBeInTheDocument();

    // Same component instance (no unmount), just a new prop value — e.g. the contract's end
    // date was edited elsewhere and ContractsPage refetched. A stale, mount-time-only
    // calculation would keep showing "10 days" here.
    rerender(<ExpiringSoonBadge effectiveEndDate={in50Days} status="ACTIVE" />);
    expect(screen.getByText('Ending in 50 days')).toBeInTheDocument();
    expect(screen.queryByText('Ending in 10 days')).not.toBeInTheDocument();
  });
});
