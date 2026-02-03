import { ContractStatus } from '@/types/contract';

interface ContractStatusBadgeProps {
  status: ContractStatus;
  className?: string;
}

const statusColors: Record<ContractStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200',
  PENDING_SIGNATURE:
    'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  EXPIRED:
    'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200',
  TERMINATED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
};

const statusLabels: Record<ContractStatus, string> = {
  DRAFT: 'Draft',
  PENDING_SIGNATURE: 'Pending Signature',
  ACTIVE: 'Active',
  EXPIRED: 'Expired',
  TERMINATED: 'Terminated',
};

export const ContractStatusBadge = ({
  status,
  className = '',
}: ContractStatusBadgeProps) => {
  return (
    <span
      className={`px-3 py-1 rounded-full text-xs font-semibold ${statusColors[status]} ${className}`}
    >
      {statusLabels[status]}
    </span>
  );
};
