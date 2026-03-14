import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import { PaymentStatus } from '@/types/payment';

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
  className?: string;
}

const statusConfig: Record<
  PaymentStatus,
  { label: string; color: BadgeColorVariant }
> = {
  PENDING: { label: 'Pending', color: 'amber' },
  PARTIALLY_PAID: { label: 'Partial', color: 'blue' },
  PAID: { label: 'Paid', color: 'emerald' },
  OVERDUE: { label: 'Overdue', color: 'red' },
  CANCELLED: { label: 'Cancelled', color: 'gray' },
};

export const PaymentStatusBadge = ({
  status,
  className = '',
}: PaymentStatusBadgeProps) => {
  const config = statusConfig[status];
  return (
    <StatusBadge
      label={config.label}
      color={config.color}
      shape="pill"
      className={className}
    />
  );
};
