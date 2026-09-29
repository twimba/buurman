import type { SignatureRequestResponse } from '@/generated/models';

const LABELS: Record<SignatureRequestResponse['status'], string> = {
  PENDING: 'Pending',
  PARTIALLY_SIGNED: 'Partially signed',
  COMPLETED: 'Signed',
  DECLINED: 'Declined',
  CANCELLED: 'Cancelled',
  FAILED: 'Failed',
};

const COLORS: Record<SignatureRequestResponse['status'], string> = {
  PENDING: 'bg-warning-bg text-warning-text',
  PARTIALLY_SIGNED: 'bg-info-bg text-info-text',
  COMPLETED: 'bg-success-bg text-success-text',
  DECLINED: 'bg-error-bg text-error-text',
  CANCELLED: 'bg-surface-inset text-text-secondary',
  FAILED: 'bg-error-bg text-error-text',
};

interface SignatureStatusBadgeProps {
  status: SignatureRequestResponse['status'];
}

export const SignatureStatusBadge = ({
  status,
}: SignatureStatusBadgeProps) => (
  <span
    className={`px-2 py-0.5 text-xs font-medium rounded-full ${COLORS[status]}`}
  >
    {LABELS[status]}
  </span>
);
