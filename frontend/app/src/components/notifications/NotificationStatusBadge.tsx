import { useTranslation } from 'react-i18next';
import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import { NotificationStatus } from '@/types/notification';

const statusConfig: Record<
  string,
  { labelKey: string; color: BadgeColorVariant }
> = {
  [NotificationStatus.PENDING]: {
    labelKey: 'notifications.statuses.pending',
    color: 'gray',
  },
  [NotificationStatus.QUEUED]: {
    labelKey: 'notifications.statuses.queued',
    color: 'blue',
  },
  [NotificationStatus.SENT]: {
    labelKey: 'notifications.statuses.sent',
    color: 'cyan',
  },
  [NotificationStatus.DELIVERED]: {
    labelKey: 'notifications.statuses.delivered',
    color: 'emerald',
  },
  [NotificationStatus.FAILED]: {
    labelKey: 'notifications.statuses.failed',
    color: 'red',
  },
  [NotificationStatus.BOUNCED]: {
    labelKey: 'notifications.statuses.bounced',
    color: 'amber',
  },
  [NotificationStatus.REJECTED]: {
    labelKey: 'notifications.statuses.rejected',
    color: 'red',
  },
  [NotificationStatus.DEMO_BLOCKED]: {
    labelKey: 'notifications.statuses.demo',
    color: 'violet',
  },
};

interface NotificationStatusBadgeProps {
  status: string;
}

export const NotificationStatusBadge = ({
  status,
}: NotificationStatusBadgeProps) => {
  const { t } = useTranslation('admin');
  const config = statusConfig[status] ?? {
    labelKey: status,
    color: 'gray' as BadgeColorVariant,
  };
  return (
    <StatusBadge label={t(config.labelKey)} color={config.color} shape="pill" />
  );
};
