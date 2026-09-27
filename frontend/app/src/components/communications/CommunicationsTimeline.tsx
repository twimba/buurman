import { useTranslation } from 'react-i18next';
import type { CommunicationResponse } from '@/generated/models';

interface CommunicationsTimelineProps {
  communications: CommunicationResponse[];
  isLoading: boolean;
  isError?: boolean;
  onResend?: (identifier: string) => void;
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

export const CommunicationsTimeline = ({
  communications,
  isLoading,
  isError = false,
  onResend,
}: CommunicationsTimelineProps) => {
  const { t, i18n } = useTranslation('common');

  if (isLoading) {
    return <p className="text-sm text-text-secondary">{t('buttons.loading')}</p>;
  }

  // Distinct from the empty state: "nothing sent yet" is a confident claim, and a failed
  // request must not make it — a landlord would send a duplicate reminder.
  if (isError) {
    return <p className="text-sm text-text-secondary">{t('communications.loadError')}</p>;
  }

  if (communications.length === 0) {
    return (
      <div className="text-sm text-text-secondary">
        <p>{t('communications.empty')}</p>
        <p className="text-xs text-text-muted">{t('communications.emptyHistorical')}</p>
      </div>
    );
  }

  return (
    <ul className="divide-y divide-border-default">
      {communications.map((communication) => (
        <li key={communication.identifier} className="py-3">
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-sm font-medium text-text-primary">
              {t(`communications.type.${communication.notificationType}`, {
                defaultValue: communication.notificationType,
              })}
            </span>
            <span className="text-sm text-text-secondary">
              {communication.recipientEmail ?? communication.recipientPhone ?? ''}
            </span>
            <time
              dateTime={communication.createdAt}
              className="text-xs text-text-secondary"
              title={new Date(communication.createdAt).toLocaleString(i18n.language)}
            >
              {new Date(communication.createdAt).toLocaleDateString(i18n.language, {
                day: 'numeric',
                month: 'short',
                year: 'numeric',
              })}
            </time>
            <span className="ml-auto text-xs text-text-secondary">
              {t(`communications.status.${statusKey(communication)}`)}
            </span>
            {onResend && (
              <button
                type="button"
                onClick={() => onResend(communication.identifier)}
                className="text-xs text-primary-600 hover:underline"
              >
                {t('communications.resend')}
              </button>
            )}
          </div>
          {communication.subject && (
            <p className="mt-1 text-sm text-text-secondary">{communication.subject}</p>
          )}
          {/* A bounce the landlord cannot explain is one they cannot act on. */}
          {communication.providerError && (
            <p className="mt-1 text-xs text-danger-600">{communication.providerError}</p>
          )}
        </li>
      ))}
    </ul>
  );
};
