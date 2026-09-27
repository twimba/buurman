import { useTranslation } from 'react-i18next';
import type { CommunicationResponse } from '@/generated/models';

interface CommunicationsTimelineProps {
  communications: CommunicationResponse[];
  isLoading: boolean;
  onResend?: (identifier: string) => void;
}

/** Collapses the seven-value status enum into what a landlord can act on. */
const statusKey = (communication: CommunicationResponse): string => {
  if (communication.status === 'DELIVERED' && communication.opened) {
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
    default:
      return 'failed';
  }
};

export const CommunicationsTimeline = ({
  communications,
  isLoading,
  onResend,
}: CommunicationsTimelineProps) => {
  const { t } = useTranslation('common');

  if (isLoading) {
    return <p className="text-sm text-text-secondary">{t('buttons.loading')}</p>;
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
        <li key={communication.identifier} className="flex items-center gap-3 py-3">
          <span className="text-sm font-medium text-text-primary">
            {communication.notificationType}
          </span>
          <span className="text-sm text-text-secondary">
            {communication.recipientEmail ?? communication.recipientPhone ?? ''}
          </span>
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
        </li>
      ))}
    </ul>
  );
};
