import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import type { ExtensionStatus } from '@/types/contractExtension';

interface ExtensionStatusBadgeProps {
  status: ExtensionStatus;
  className?: string;
}

const statusConfig: Record<
  ExtensionStatus,
  { label: string; color: BadgeColorVariant }
> = {
  DRAFT: { label: 'Draft', color: 'amber' },
  ACTIVE: { label: 'Active', color: 'emerald' },
  SUPERSEDED: { label: 'Superseded', color: 'gray' },
  CANCELLED: { label: 'Cancelled', color: 'red' },
  DECLINED: { label: 'Declined', color: 'orange' },
};

export const ExtensionStatusBadge = ({
  status,
  className = '',
}: ExtensionStatusBadgeProps) => {
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
