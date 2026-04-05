import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import type { ExtensionStatus } from '@/types/contractExtension';
import { useTranslation } from 'react-i18next';

interface ExtensionStatusBadgeProps {
  status: ExtensionStatus;
  className?: string;
}

const statusColors: Record<ExtensionStatus, BadgeColorVariant> = {
  DRAFT: 'amber',
  ACTIVE: 'emerald',
  SUPERSEDED: 'gray',
  CANCELLED: 'red',
  DECLINED: 'orange',
};

export const ExtensionStatusBadge = ({
  status,
  className = '',
}: ExtensionStatusBadgeProps) => {
  const { t } = useTranslation('contracts');
  return (
    <StatusBadge
      label={t(`enums.extensionStatuses.${status}`)}
      color={statusColors[status]}
      shape="pill"
      className={className}
    />
  );
};
