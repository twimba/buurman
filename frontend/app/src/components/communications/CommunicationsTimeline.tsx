import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, Mail, RotateCcw, Send, Smartphone } from 'lucide-react';
import {
  ConfirmDialog,
  EmptyState,
  LoadingSpinner,
  StatusBadge,
  type BadgeColorVariant,
} from '@buurman/ui';
import type { CommunicationResponse } from '@/generated/models';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useFormatDate } from '@/hooks/useFormatDate';

/** Rows shown before the "show all" expander; a long-lived contract accumulates many. */
const COLLAPSED_ROWS = 5;

/**
 * Matches NotificationRepository.TIMELINE_LIMIT. At exactly this many rows the server has
 * truncated, and saying so matters more than paging: a landlord who believes they are seeing
 * everything will conclude a message was never sent.
 */
const SERVER_LIMIT = 100;

interface CommunicationsTimelineProps {
  communications: CommunicationResponse[];
  isLoading: boolean;
  isError?: boolean;
  onResend?: (identifier: string) => void;
  resendingIdentifier?: string;
}

/**
 * Collapses the status enum into what a landlord can act on.
 *
 * `opened` is checked before `status` on purpose: Mailgun does not guarantee event order, and an
 * `opened` webhook that lands before `delivered` updates the open columns without touching status.
 */
const statusKey = (communication: CommunicationResponse): string => {
  if (communication.opened) {
    return 'opened';
  }
  switch (communication.status) {
    case 'PENDING':
    case 'QUEUED':
      return 'queued';
    case 'SENT':
      return 'sent';
    case 'DELIVERED':
      return 'delivered';
    case 'BOUNCED':
    case 'REJECTED':
      return 'notDelivered';
    case 'DEMO_BLOCKED':
      // Nothing failed: the team has notification delivery switched off.
      return 'demoBlocked';
    default:
      return 'failed';
  }
};

const statusColor: Record<string, BadgeColorVariant> = {
  queued: 'gray',
  sent: 'blue',
  delivered: 'emerald',
  opened: 'violet',
  notDelivered: 'red',
  failed: 'red',
  demoBlocked: 'amber',
};

/**
 * The rail node is the whole panel's glance value: channel icon, tinted by outcome. Scanning down
 * the rail answers "did anything fail" without reading a word, so only a failure gets a warm tint.
 */
const nodeTint = (key: string): string => {
  if (key === 'notDelivered' || key === 'failed') {
    return 'bg-error-bg text-error-text';
  }
  if (key === 'delivered' || key === 'opened') {
    return 'bg-success-bg text-success-text';
  }
  return 'bg-primary-50 dark:bg-primary-950 text-primary-600 dark:text-primary-300';
};

export const CommunicationsTimeline = ({
  communications,
  isLoading,
  isError = false,
  onResend,
  resendingIdentifier,
}: CommunicationsTimelineProps) => {
  const { t } = useTranslation('common');
  const { formatDateTime, formatRelative } = useFormatDate();
  const [expanded, setExpanded] = useState(false);
  const [pendingResend, setPendingResend] = useState<CommunicationResponse>();

  // One send fans out to an internal copy per team member alongside the tenant's. Only the
  // tenant-facing rows answer "was my tenant told", so they are the timeline; the copies collapse
  // into a single line. Resend is offered on contact rows only — resending an internal copy sends
  // a colleague a duplicate while the landlord believes they are chasing the tenant.
  const contactRows = useMemo(
    () => communications.filter((row) => row.audience === 'CONTACT'),
    [communications]
  );
  const internalCount = communications.length - contactRows.length;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  // Distinct from the empty state: "nothing sent yet" is a confident claim, and a failed
  // request must not make it — a landlord would send a duplicate reminder.
  if (isError) {
    return <ErrorMessage message={t('communications.loadError')} />;
  }

  if (communications.length === 0) {
    return (
      <EmptyState
        variant="inline"
        icon={<Send className="h-8 w-8" />}
        title={t('communications.empty')}
        description={t('communications.emptyHistorical')}
      />
    );
  }

  const visible = expanded ? contactRows : contactRows.slice(0, COLLAPSED_ROWS);
  const hidden = contactRows.length - visible.length;

  return (
    <>
      {contactRows.length === 0 ? (
        <EmptyState
          variant="inline"
          icon={<Send className="h-8 w-8" />}
          title={t('communications.noneToContact')}
          description={t('communications.internalOnly', {
            total: internalCount,
          })}
        />
      ) : (
        <ol className="relative border-l border-border-default ml-3 space-y-6">
          {visible.map((communication) => {
            const key = statusKey(communication);
            const recipient =
              communication.recipientEmail ?? communication.recipientPhone;
            const typeLabel = t(
              `communications.type.${communication.notificationType}`,
              { defaultValue: communication.notificationType }
            );
            const isSms = communication.channel === 'SMS';

            return (
              <li key={communication.identifier} className="ml-6">
                <span
                  className={`absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full ring-4 ring-surface-card ${nodeTint(key)}`}
                >
                  {isSms ? (
                    <Smartphone className="h-3.5 w-3.5" />
                  ) : (
                    <Mail className="h-3.5 w-3.5" />
                  )}
                </span>

                <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
                  <p className="text-sm font-medium text-text-primary">
                    {typeLabel}
                    {communication.resend && (
                      <span className="ml-1.5 inline-flex items-center gap-1 text-xs font-normal text-text-muted">
                        <RotateCcw className="h-3 w-3" />
                        {t('communications.resent')}
                      </span>
                    )}
                  </p>
                  <span className="flex shrink-0 items-center gap-2">
                    <StatusBadge
                      label={t(`communications.status.${key}`)}
                      color={statusColor[key] ?? 'gray'}
                      shape="pill"
                      dot
                    />
                    <time
                      dateTime={communication.createdAt}
                      title={formatDateTime(communication.createdAt)}
                      className="text-xs text-text-muted"
                    >
                      {formatRelative(communication.createdAt)}
                    </time>
                  </span>
                </div>

                {recipient && (
                  <p className="mt-1 truncate text-xs text-text-muted">
                    {t('communications.to', { recipient })}
                  </p>
                )}

                {communication.subject && (
                  <p className="mt-1 text-sm text-text-secondary">
                    {communication.subject}
                  </p>
                )}

                {/*
                 * A bounce the landlord cannot explain is one they cannot act on — but the raw
                 * provider string is no more actionable than silence, so it moves to the title and
                 * the visible line says what it means for them.
                 */}
                {communication.providerError && (
                  <p
                    className="mt-1 flex items-center gap-1 text-xs text-error-text"
                    title={communication.providerError}
                  >
                    <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                    {t(`communications.failureReason.${key}`, {
                      defaultValue: t('communications.failureReason.failed'),
                    })}
                  </p>
                )}

                {onResend && (
                  <button
                    type="button"
                    onClick={() => setPendingResend(communication)}
                    disabled={resendingIdentifier === communication.identifier}
                    aria-label={t('communications.resendTo', {
                      type: typeLabel,
                      recipient: recipient ?? typeLabel,
                    })}
                    className="focus-ring hit-44 mt-1 rounded py-1 text-xs font-medium text-primary-600 hover:underline disabled:cursor-not-allowed disabled:opacity-60 dark:text-primary-300"
                  >
                    {resendingIdentifier === communication.identifier
                      ? t('communications.resending')
                      : t('communications.resend')}
                  </button>
                )}
              </li>
            );
          })}
        </ol>
      )}

      {hidden > 0 && (
        <button
          type="button"
          onClick={() => setExpanded(true)}
          className="focus-ring mt-4 rounded text-sm font-medium text-primary-600 hover:underline dark:text-primary-300"
        >
          {t('communications.showAll', { total: contactRows.length })}
        </button>
      )}

      {internalCount > 0 && (
        <p className="mt-4 border-t border-border-default pt-3 text-xs text-text-muted">
          {t('communications.alsoCopiedToTeam', { total: internalCount })}
        </p>
      )}

      {communications.length >= SERVER_LIMIT && (
        <p className="mt-2 text-xs text-text-muted">
          {t('communications.truncated', { total: SERVER_LIMIT })}
        </p>
      )}

      {pendingResend && onResend && (
        <ConfirmDialog
          title={t('communications.confirmResend.title')}
          message={t('communications.confirmResend.message', {
            type: t(`communications.type.${pendingResend.notificationType}`, {
              defaultValue: pendingResend.notificationType,
            }),
            recipient:
              pendingResend.recipientEmail ??
              pendingResend.recipientPhone ??
              '',
          })}
          confirmLabel={t('communications.resend')}
          cancelLabel={t('buttons.cancel')}
          isLoading={resendingIdentifier === pendingResend.identifier}
          onConfirm={() => {
            onResend(pendingResend.identifier);
            setPendingResend(undefined);
          }}
          onCancel={() => setPendingResend(undefined)}
        />
      )}
      <span className="sr-only" aria-live="polite">
        {resendingIdentifier ? t('communications.resending') : ''}
      </span>
    </>
  );
};
