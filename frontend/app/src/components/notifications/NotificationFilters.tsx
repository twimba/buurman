import { useTranslation } from 'react-i18next';
import { Search } from 'lucide-react';
import {
  NotificationType,
  NotificationChannel,
  NotificationStatus,
  NotificationFilterParams,
} from '@/types/notification';

const notificationTypeKeys: Record<NotificationType, string> = {
  [NotificationType.WELCOME]: 'notifications.types.welcome',
  [NotificationType.VERIFICATION_CODE]: 'notifications.types.verificationCode',
  [NotificationType.TEAM_INVITATION]: 'notifications.types.teamInvitation',
  [NotificationType.INVITATION_ACCEPTED]:
    'notifications.types.invitationAccepted',
  [NotificationType.PASSWORD_CHANGED]: 'notifications.types.passwordChanged',
  [NotificationType.PAYMENT_REMINDER]: 'notifications.types.paymentReminder',
  [NotificationType.CONTRACT_EXPIRY]: 'notifications.types.contractExpiry',
  [NotificationType.PROPERTY_CREATED]: 'notifications.types.propertyCreated',
  [NotificationType.CONTRACT_CREATED]: 'notifications.types.contractCreated',
  [NotificationType.CONTRACT_STATUS_CHANGED]:
    'notifications.types.contractStatusChanged',
  [NotificationType.CONTRACT_REOPENED]: 'notifications.types.contractReopened',
  [NotificationType.PAYMENT_PAID]: 'notifications.types.paymentPaid',
  [NotificationType.PAYMENT_RECEIVAL]: 'notifications.types.paymentReceival',
  [NotificationType.EXPENSE_CREATED]: 'notifications.types.expenseCreated',
};

interface NotificationFiltersProps {
  filters: NotificationFilterParams;
  onFilterChange: (filters: NotificationFilterParams) => void;
}

export const NotificationFilters = ({
  filters,
  onFilterChange,
}: NotificationFiltersProps) => {
  const { t } = useTranslation('admin');
  const selectClass =
    'border border-border-strong rounded-lg px-3 py-2 bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30';

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted " />
        <input
          type="text"
          placeholder={t('notifications.filters.searchByEmail')}
          value={filters.recipientEmail ?? ''}
          onChange={(e) =>
            onFilterChange({
              ...filters,
              recipientEmail: e.target.value || undefined,
            })
          }
          className="pl-9 pr-3 py-2 border border-border-strong rounded-lg bg-surface-card text-sm text-text-primary placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/30 w-56"
        />
      </div>

      <select
        value={filters.type ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            type: (e.target.value as NotificationType) || undefined,
          })
        }
        className={selectClass}
      >
        <option value="">{t('notifications.filters.allTypes')}</option>
        {Object.entries(notificationTypeKeys).map(([value, key]) => (
          <option key={value} value={value}>
            {t(key)}
          </option>
        ))}
      </select>

      <select
        value={filters.channel ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            channel: (e.target.value as NotificationChannel) || undefined,
          })
        }
        className={selectClass}
      >
        <option value="">{t('notifications.filters.allChannels')}</option>
        <option value={NotificationChannel.EMAIL}>Email</option>
        <option value={NotificationChannel.SMS}>SMS</option>
      </select>

      <select
        value={filters.status ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            status: (e.target.value as NotificationStatus) || undefined,
          })
        }
        className={selectClass}
      >
        <option value="">{t('notifications.filters.allStatuses')}</option>
        {Object.values(NotificationStatus).map((status) => (
          <option key={status} value={status}>
            {status.charAt(0) + status.slice(1).toLowerCase()}
          </option>
        ))}
      </select>

      <input
        type="date"
        value={filters.dateFrom ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            dateFrom: e.target.value || undefined,
          })
        }
        className={selectClass}
        title={t('notifications.filters.fromDate')}
      />
      <input
        type="date"
        value={filters.dateTo ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            dateTo: e.target.value || undefined,
          })
        }
        className={selectClass}
        title={t('notifications.filters.toDate')}
      />
    </div>
  );
};
