import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';
import { Button, ModalWrapper } from '@buurman/ui';
import { useReminderPreview } from '@/hooks/useTeamHooks';
import type { ReminderTone } from '@/generated/models';

interface ReminderPreviewModalProps {
  open: boolean;
  teamId: string | undefined;
  tone: ReminderTone;
  offsetDays: number;
  onClose: () => void;
}

/**
 * Shows the email a tenant would receive for one ladder step. The body is rendered by the same
 * template the real send uses, and is displayed in a sandboxed iframe so the email's own CSS
 * cannot leak into the app and its markup is never executed as part of this page.
 */
export const ReminderPreviewModal = ({
  open,
  teamId,
  tone,
  offsetDays,
  onClose,
}: ReminderPreviewModalProps) => {
  const { t } = useTranslation('settings');
  const { data, isLoading, isError } = useReminderPreview(
    open ? teamId : undefined,
    open ? tone : undefined,
    offsetDays
  );

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={t('tenantReminders.preview.title')}
      subtitle={t('tenantReminders.preview.subtitle', {
        tone: t(`tenantReminders.tones.${tone}`),
      })}
      size="lg"
      footer={
        <Button variant="secondary" onClick={onClose}>
          {t('common:buttons.close')}
        </Button>
      }
    >
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-primary-500 dark:text-primary-300" />
        </div>
      ) : isError || !data ? (
        <p className="text-sm text-error-text py-6">
          {t('tenantReminders.preview.failed')}
        </p>
      ) : (
        <div className="space-y-3">
          <div className="rounded-md border border-info-border bg-info-bg px-3 py-2 text-xs text-info-text">
            {t('tenantReminders.preview.sampleNotice', {
              name: data.sampleTenantName,
              language: data.languageTag.toUpperCase(),
            })}
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-text-muted mb-1">
              {t('tenantReminders.preview.subject')}
            </p>
            <p className="text-sm font-medium text-text-primary">
              {data.subject}
            </p>
          </div>
          <iframe
            title={t('tenantReminders.preview.title')}
            sandbox=""
            srcDoc={data.html}
            className="w-full h-[26rem] rounded-md border border-border-default bg-white"
          />
        </div>
      )}
    </ModalWrapper>
  );
};
