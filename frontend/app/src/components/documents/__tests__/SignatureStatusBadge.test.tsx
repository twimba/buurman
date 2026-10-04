import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SignatureStatusBadge } from '../SignatureStatusBadge';

describe('SignatureStatusBadge', () => {
  it.each([
    ['PENDING', 'Pending'],
    ['PARTIALLY_SIGNED', 'Partially signed'],
    ['COMPLETED', 'Signed'],
    ['DECLINED', 'Declined'],
    ['CANCELLED', 'Cancelled'],
    ['FAILED', 'Failed'],
  ] as const)('renders %s as "%s"', (status, label) => {
    render(<SignatureStatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
