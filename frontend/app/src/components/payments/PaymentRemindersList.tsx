import { useTranslation } from 'react-i18next';
import { Bot, Mail, Send, UserRound } from 'lucide-react';
import { EmptyState } from '@buurman/ui';
import type { PaymentReminderResponse } from '@/types/payment';
import { useFormatDate } from '@/hooks/useFormatDate';

interface PaymentRemindersListProps {
  reminders: PaymentReminderResponse[];
  formatMoney: (value: number, currency: string) => string;
}

/** Communications timeline: every reminder sent to the tenant for one payment, newest first. */
export const PaymentRemindersList = ({
  reminders,
  formatMoney,
}: PaymentRemindersListProps) => {
  const { t } = useTranslation('payments');
  const { formatDateTime, formatRelative } = useFormatDate();

  if (reminders.length === 0) {
    return (
      <EmptyState
        variant="inline"
        icon={<Send className="h-8 w-8" />}
        title={t('reminders.empty')}
        description={t('reminders.emptySubtitle')}
      />
    );
  }

  return (
    <ol className="relative border-l border-border-default ml-3 space-y-6">
      {reminders.map((reminder) => {
        const automatic = reminder.reminderType === 'AUTOMATIC';
        return (
          <li key={reminder.identifier} className="ml-6">
            <span className="absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full bg-primary-50 dark:bg-primary-950 ring-4 ring-surface-card">
              {automatic ? (
                <Bot className="h-3.5 w-3.5 text-primary-600 dark:text-primary-300" />
              ) : (
                <UserRound className="h-3.5 w-3.5 text-primary-600 dark:text-primary-300" />
              )}
            </span>
            <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
              <p className="text-sm font-medium text-text-primary">
                {automatic ? t('reminders.automatic') : t('reminders.manual')}
                {reminder.sentByName && !automatic && (
                  <span className="text-text-secondary font-normal">
                    {' '}
                    {t('reminders.sentBy', { name: reminder.sentByName })}
                  </span>
                )}
              </p>
              <time
                dateTime={reminder.sentAt}
                title={formatDateTime(reminder.sentAt)}
                className="text-xs text-text-muted"
              >
                {formatRelative(reminder.sentAt)}
              </time>
            </div>
            <p className="mt-1 text-sm text-text-secondary flex flex-wrap items-center gap-x-2">
              {reminder.recipientEmail && (
                <span className="inline-flex items-center gap-1">
                  <Mail className="h-3.5 w-3.5" />
                  {t('reminders.to', { email: reminder.recipientEmail })}
                </span>
              )}
              <span>
                {t('reminders.outstanding', {
                  amount: formatMoney(
                    reminder.outstandingAmount,
                    reminder.currency
                  ),
                })}
              </span>
              {reminder.daysOverdue > 0 && (
                <span className="text-error-text">
                  {t('reminders.daysOverdue', { days: reminder.daysOverdue })}
                </span>
              )}
            </p>
            {reminder.notes && (
              <blockquote className="mt-2 border-l-2 border-border-default pl-3 text-sm text-text-secondary whitespace-pre-line">
                {reminder.notes}
              </blockquote>
            )}
          </li>
        );
      })}
    </ol>
  );
};
