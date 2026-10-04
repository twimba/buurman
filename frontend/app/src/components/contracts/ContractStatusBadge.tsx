import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import { useTranslation } from 'react-i18next';
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
  NOTICE_GIVEN: { label: 'Notice Given', color: 'amber' },
};

export const ContractStatusBadge = ({
  status,
  className = '',
}: ContractStatusBadgeProps) => {
  const { t } = useTranslation('contracts');
  const config = statusConfig[status];
  return (
    <StatusBadge
      label={t('statusChange.statuses.' + status)}
      color={config.color}
      shape="pill"
      className={className}
    />
  );
};
