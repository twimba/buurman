import { useTranslation } from 'react-i18next';
import { Button, LoadingSpinner, Sheet } from '@buurman/ui';
import type { CommunicationResponse } from '@/generated/models';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  useCommunicationBody,
  type CommunicationEntity,
} from '@/hooks/useCommunications';
import { MessageBodyPreview } from './MessageBodyPreview';

interface CommunicationMessageSheetProps {
  communication: CommunicationResponse;
  entity: CommunicationEntity;
  entityIdentifier: string;
  onClose: () => void;
}

/**
 * What the tenant actually received, for one row of the timeline.
 *
 * <p>A Sheet rather than an inline expansion: the rail's whole glance value is that you scan down
 * it and see whether anything failed without reading, and dropping a 400px email into it pushes
 * the other rows off screen. It is also what this app already does for a tenant-facing message —
 * settings/ReminderPreviewModal shows the same content the same way, so a landlord who previews a
 * reminder there and a sent one here meets one object, not two.
 *
 * <p>Deliberately not the admin delivery log's modal. That one answers an operator's questions —
 * provider ids, open and click counts, status transitions, refresh-from-provider. The landlord's
 * question is only ever "what did my tenant get?", and everything else the row already told them.
 */
export const CommunicationMessageSheet = ({
  communication,
  entity,
  entityIdentifier,
  onClose,
}: CommunicationMessageSheetProps) => {
  const { t } = useTranslation('common');
  const { formatDateTime } = useFormatDate();
  const { data, isLoading, isError, refetch } = useCommunicationBody(
    entity,
    entityIdentifier,
    communication.identifier
  );

  const recipient =
    communication.recipientEmail ?? communication.recipientPhone;
  const sentAt = formatDateTime(communication.createdAt);

  return (
    <Sheet
      open
      onClose={onClose}
      className="md:max-w-2xl"
      title={t(`communications.type.${communication.notificationType}`, {
        defaultValue: communication.notificationType,
      })}
      // The row's own facts, so the sheet identifies itself the moment it opens rather than after
      // the body arrives — and so a screen-reader user landing inside the dialog is not stranded.
      description={
        recipient
          ? t('communications.preview.sentToOn', { recipient, date: sentAt })
          : t('communications.preview.sentOn', { date: sentAt })
      }
      footer={
        <div className="flex justify-end gap-2">
          {isError && (
            <Button variant="secondary" onClick={() => refetch()}>
              {t('communications.preview.retry')}
            </Button>
          )}
          <Button onClick={onClose}>{t('buttons.close')}</Button>
        </div>
      }
    >
      {communication.status === 'DEMO_BLOCKED' && (
        <p className="mb-4 rounded-md bg-info-bg border border-info-border px-3 py-2 text-sm text-info-text">
          {t('communications.preview.demoBlocked')}
        </p>
      )}

      {data?.subject && (
        <div className="mb-4">
          <p className="text-[10px] uppercase tracking-wide text-text-muted">
            {t('communications.preview.subject')}
          </p>
          <p className="text-sm font-medium text-text-primary">
            {data.subject}
          </p>
        </div>
      )}

      <div aria-busy={isLoading} className="min-h-[16rem]">
        {isLoading && (
          <div className="flex items-center justify-center py-16">
            <LoadingSpinner />
          </div>
        )}

        {isError && (
          <ErrorMessage message={t('communications.preview.loadError')} />
        )}

        {/* body is non-optional server-side, but an older row can still carry an empty string. */}
        {!isLoading && !isError && !data?.body && (
          <p className="py-16 text-center text-sm text-text-muted">
            {t('communications.preview.noContent')}
          </p>
        )}

        {!isLoading && !isError && data?.body && (
          <MessageBodyPreview channel={data.channel} body={data.body} />
        )}
      </div>
    </Sheet>
  );
};
