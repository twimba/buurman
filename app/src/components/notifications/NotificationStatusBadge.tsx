import { NotificationStatus } from '@/types/notification';

const statusConfig: Record<
  NotificationStatus,
  { label: string; className: string }
> = {
  [NotificationStatus.PENDING]: {
    label: 'Pending',
    className:
      'bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700',
  },
  [NotificationStatus.QUEUED]: {
    label: 'Queued',
    className:
      'bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700',
  },
  [NotificationStatus.SENT]: {
    label: 'Sent',
    className:
      'bg-sky-50 text-sky-700 ring-1 ring-sky-200 dark:bg-sky-900/30 dark:text-sky-300 dark:ring-sky-700',
  },
  [NotificationStatus.DELIVERED]: {
    label: 'Delivered',
    className:
      'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700',
  },
  [NotificationStatus.FAILED]: {
    label: 'Failed',
    className:
      'bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700',
  },
  [NotificationStatus.BOUNCED]: {
    label: 'Bounced',
    className:
      'bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700',
  },
  [NotificationStatus.REJECTED]: {
    label: 'Rejected',
    className:
      'bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700',
  },
  [NotificationStatus.DEMO_BLOCKED]: {
    label: 'Demo',
    className:
      'bg-violet-50 text-violet-700 ring-1 ring-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:ring-violet-700',
  },
};

interface NotificationStatusBadgeProps {
  status: NotificationStatus;
}

export const NotificationStatusBadge = ({
  status,
}: NotificationStatusBadgeProps) => {
  const config = statusConfig[status] ?? {
    label: status,
    className:
      'bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700',
  };

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
};
