import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import { NotificationStatus } from '@/types/notification';

const statusConfig: Record<
  NotificationStatus,
  { label: string; color: BadgeColorVariant }
> = {
  [NotificationStatus.PENDING]: { label: 'Pending', color: 'gray' },
  [NotificationStatus.QUEUED]: { label: 'Queued', color: 'blue' },
  [NotificationStatus.SENT]: { label: 'Sent', color: 'cyan' },
  [NotificationStatus.DELIVERED]: { label: 'Delivered', color: 'emerald' },
  [NotificationStatus.FAILED]: { label: 'Failed', color: 'red' },
  [NotificationStatus.BOUNCED]: { label: 'Bounced', color: 'amber' },
  [NotificationStatus.REJECTED]: { label: 'Rejected', color: 'red' },
  [NotificationStatus.DEMO_BLOCKED]: { label: 'Demo', color: 'violet' },
};

interface NotificationStatusBadgeProps {
  status: NotificationStatus;
}

export const NotificationStatusBadge = ({
  status,
}: NotificationStatusBadgeProps) => {
  const config = statusConfig[status] ?? {
    label: status,
    color: 'gray' as BadgeColorVariant,
  };
  return <StatusBadge label={config.label} color={config.color} shape="pill" />;
};
