import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import { ContractStatus } from '@/types/contract';

interface ContractStatusBadgeProps {
  status: ContractStatus;
  className?: string;
}

const statusConfig: Record<
  ContractStatus,
  { label: string; color: BadgeColorVariant }
> = {
  DRAFT: { label: 'Draft', color: 'gray' },
  PENDING_SIGNATURE: { label: 'Pending Signature', color: 'blue' },
  ACTIVE: { label: 'Active', color: 'emerald' },
  EXPIRED: { label: 'Expired', color: 'orange' },
  TERMINATED: { label: 'Terminated', color: 'red' },
};

export const ContractStatusBadge = ({
  status,
  className = '',
}: ContractStatusBadgeProps) => {
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
