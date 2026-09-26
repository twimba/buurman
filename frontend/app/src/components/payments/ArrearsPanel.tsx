import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { AlertTriangle, ChevronDown, ChevronUp, Send } from 'lucide-react';
import { Button } from '@buurman/ui';
import type { PaymentArrearsResponse, ContactArrears } from '@/types/payment';
import { useFormatDate } from '@/hooks/useFormatDate';

interface ArrearsPanelProps {
  arrears: PaymentArrearsResponse;
  formatMoney: (value: number, currency: string) => string;
  canEdit: boolean;
  onSendReminders: (paymentIdentifiers: string[]) => void;
  sendingFor?: string[] | null;
}

const COLLAPSED_ROWS = 5;

const bucketTone: Record<string, string> = {
  '1-30': 'bg-warning-bg text-warning-text border-warning-border',
  '31-60': 'bg-warning-bg text-warning-text border-warning-border',
  '61-90': 'bg-error-bg text-error-text border-error-border',
  '91+': 'bg-error-bg text-error-text border-error-border',
};

/**
 * Team-wide arrears: ageing buckets plus the tenants who owe the most, each with a one-click
 * reminder. Renders nothing when there is nothing overdue — the stats card already says so.
 */
export const ArrearsPanel = ({
  arrears,
  formatMoney,
  canEdit,
  onSendReminders,
  sendingFor,
}: ArrearsPanelProps) => {
  const { t } = useTranslation('payments');
  const { formatRelative } = useFormatDate();
  const [expanded, setExpanded] = useState(false);

  if (arrears.paymentCount === 0) {
    return null;
  }

  const currency = arrears.currency ?? '';
  const rows = expanded
    ? arrears.contacts
    : arrears.contacts.slice(0, COLLAPSED_ROWS);

  const tenantName = (row: ContactArrears) =>
    row.contact?.displayName ?? t('arrears.noContact');

  return (
    <section
      aria-labelledby="arrears-heading"
      className="mb-6 bg-surface-card rounded-lg shadow-sm border border-border-default overflow-hidden"
    >
      <div className="px-4 md:px-6 py-4 border-b border-border-default flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-error-text" />
          <div>
            <h3
              id="arrears-heading"
              className="text-sm font-semibold text-text-primary"
            >
              {t('arrears.title')}
            </h3>
            <p className="text-xs text-text-secondary">
              {t('arrears.summary', {
                amount: formatMoney(arrears.totalOutstanding, currency),
                count: arrears.contactCount,
              })}
              {' · '}
              {t('arrears.oldest', { days: arrears.oldestDaysOverdue })}
            </p>
          </div>
        </div>
        {canEdit && (
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<Send />}
            isLoading={
              sendingFor === null
                ? false
                : sendingFor?.length === arrears.paymentCount
            }
            disabled={!arrears.contacts.some((c) => c.remindersEnabled)}
            title={
              arrears.contacts.some((c) => c.remindersEnabled)
                ? undefined
                : t('arrears.remindersDisabled')
            }
            onClick={() =>
              onSendReminders(
                arrears.contacts
                  .filter((c) => c.remindersEnabled)
                  .flatMap((c) => c.paymentIdentifiers)
              )
            }
          >
            {t('arrears.remindAll', {
              count: arrears.contacts.filter((c) => c.remindersEnabled).length,
            })}
          </Button>
        )}
      </div>

      {/* Ageing buckets */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-2 md:gap-3 px-4 md:px-6 py-4">
        {arrears.buckets.map((bucket) => (
          <div
            key={bucket.key}
            className={`rounded-md border px-3 py-2 ${
              bucket.count > 0
                ? (bucketTone[bucket.key] ??
                  'bg-surface-inset text-text-primary border-border-default')
                : 'bg-surface-inset text-text-muted border-border-default'
            }`}
          >
            <p className="text-xs font-medium">
              {t(`arrears.bucket.${bucket.key}`, {
                defaultValue: `${bucket.key} days`,
              })}
            </p>
            <p className="text-lg font-bold tabular-nums leading-tight">
              {formatMoney(bucket.amount, currency)}
            </p>
            <p className="text-xs opacity-80">
              {t('arrears.payments', { count: bucket.count })}
            </p>
          </div>
        ))}
      </div>

      {/* Tenants behind */}
      <ul className="divide-y divide-border-default border-t border-border-default">
        {rows.map((row) => {
          const isSending =
            !!sendingFor &&
            row.paymentIdentifiers.every((id) => sendingFor.includes(id));
          return (
            <li
              key={row.paymentIdentifiers.join(',')}
              className="px-4 md:px-6 py-3 flex flex-wrap items-center gap-x-4 gap-y-2"
            >
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-text-primary truncate">
                  {row.contact ? (
                    <Link
                      to={`/contacts/${row.contact.identifier}`}
                      className="hover:underline focus-ring rounded"
                    >
                      {tenantName(row)}
                    </Link>
                  ) : (
                    tenantName(row)
                  )}
                </p>
                <p className="text-xs text-text-secondary truncate">
                  {row.property
                    ? `${row.property.street}, ${row.property.city}`
                    : ''}
                  {row.property ? ' · ' : ''}
                  {t('arrears.payments', { count: row.paymentCount })}
                  {' · '}
                  {row.lastReminderAt
                    ? t('arrears.lastReminded', {
                        when: formatRelative(row.lastReminderAt),
                        count: row.reminderCount,
                      })
                    : t('arrears.neverReminded')}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-text-primary tabular-nums">
                  {formatMoney(row.outstanding, currency)}
                </p>
                <p className="text-xs text-error-text">
                  {t('arrears.daysOverdue', { days: row.daysOverdue })}
                </p>
              </div>
              {canEdit && (
                <Button
                  variant="ghost"
                  size="sm"
                  leftIcon={<Send />}
                  isLoading={isSending}
                  disabled={!row.remindersEnabled}
                  title={
                    row.remindersEnabled
                      ? undefined
                      : t('arrears.remindersDisabled')
                  }
                  onClick={() => onSendReminders(row.paymentIdentifiers)}
                  aria-label={t('arrears.sendReminderTo', {
                    name: tenantName(row),
                  })}
                >
                  {t('arrears.sendReminder')}
                </Button>
              )}
            </li>
          );
        })}
      </ul>
      {arrears.contacts.length > COLLAPSED_ROWS && (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="w-full px-6 py-2 text-xs font-medium text-text-secondary hover:bg-surface-inset border-t border-border-default inline-flex items-center justify-center gap-1 min-h-touch focus-ring"
        >
          {expanded ? (
            <>
              <ChevronUp className="h-4 w-4" />
              {t('arrears.showLess')}
            </>
          ) : (
            <>
              <ChevronDown className="h-4 w-4" />
              {t('arrears.showAll', { count: arrears.contacts.length })}
            </>
          )}
        </button>
      )}
    </section>
  );
};
