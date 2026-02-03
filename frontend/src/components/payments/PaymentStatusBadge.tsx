import { PaymentStatus } from '@/types/payment';

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
  className?: string;
}

const statusColors: Record<PaymentStatus, string> = {
  PENDING:
    'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  PAID: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  OVERDUE: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  CANCELLED: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200',
};

const statusLabels: Record<PaymentStatus, string> = {
  PENDING: 'Pending',
  PAID: 'Paid',
  OVERDUE: 'Overdue',
  CANCELLED: 'Cancelled',
};

export const PaymentStatusBadge = ({
  status,
  className = '',
}: PaymentStatusBadgeProps) => {
  return (
    <span
      className={`px-3 py-1 rounded-full text-xs font-semibold ${statusColors[status]} ${className}`}
    >
      {statusLabels[status]}
    </span>
  );
};
