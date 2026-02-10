import { Search } from 'lucide-react';
import {
  NotificationType,
  NotificationChannel,
  NotificationStatus,
  NotificationFilterParams,
} from '@/types/notification';

const notificationTypeLabels: Record<NotificationType, string> = {
  [NotificationType.WELCOME]: 'Welcome',
  [NotificationType.VERIFICATION_CODE]: 'Verification Code',
  [NotificationType.TEAM_INVITATION]: 'Team Invitation',
  [NotificationType.INVITATION_ACCEPTED]: 'Invitation Accepted',
  [NotificationType.PASSWORD_CHANGED]: 'Password Changed',
  [NotificationType.PAYMENT_REMINDER]: 'Payment Reminder',
  [NotificationType.CONTRACT_EXPIRY]: 'Contract Expiry',
  [NotificationType.PROPERTY_CREATED]: 'Property Created',
  [NotificationType.CONTRACT_CREATED]: 'Contract Created',
  [NotificationType.CONTRACT_STATUS_CHANGED]: 'Contract Status Changed',
  [NotificationType.CONTRACT_REOPENED]: 'Contract Reopened',
  [NotificationType.PAYMENT_PAID]: 'Payment Paid',
  [NotificationType.PAYMENT_RECEIVAL]: 'Payment Receival',
  [NotificationType.EXPENSE_CREATED]: 'Expense Created',
};

interface NotificationFiltersProps {
  filters: NotificationFilterParams;
  onFilterChange: (filters: NotificationFilterParams) => void;
}

export const NotificationFilters = ({
  filters,
  onFilterChange,
}: NotificationFiltersProps) => {
  const selectClass =
    'border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg px-3 py-2 bg-white dark:bg-[#14161f] text-sm text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]/30';

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
        <input
          type="text"
          placeholder="Search by email..."
          value={filters.recipientEmail ?? ''}
          onChange={(e) =>
            onFilterChange({
              ...filters,
              recipientEmail: e.target.value || undefined,
            })
          }
          className="pl-9 pr-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-white dark:bg-[#14161f] text-sm text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#9ca0b8] dark:placeholder-[#5c6180] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]/30 w-56"
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
        <option value="">All Types</option>
        {Object.entries(notificationTypeLabels).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
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
        <option value="">All Channels</option>
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
        <option value="">All Statuses</option>
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
        title="From date"
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
        title="To date"
      />
    </div>
  );
};
