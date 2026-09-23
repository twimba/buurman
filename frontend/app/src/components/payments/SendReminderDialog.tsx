import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Send } from 'lucide-react';
import { Button, ModalWrapper, Textarea } from '@buurman/ui';

interface SendReminderDialogProps {
  open: boolean;
  /** Number of payments the reminder will be sent for. 1 = single payment wording. */
  count: number;
  isLoading?: boolean;
  onConfirm: (notes?: string) => Promise<void> | void;
  onClose: () => void;
}

export const SendReminderDialog = ({
  open,
  count,
  isLoading,
  onConfirm,
  onClose,
}: SendReminderDialogProps) => {
  const { t } = useTranslation('payments');
  const [notes, setNotes] = useState('');
  const bulk = count > 1;

  const submit = () => {
    void onConfirm(notes.trim() || undefined);
  };

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={
        bulk
          ? t('sendReminderDialog.titleBulk', { count })
          : t('sendReminderDialog.title')
      }
      subtitle={
        bulk
          ? t('sendReminderDialog.messageBulk')
          : t('sendReminderDialog.message')
      }
      size="sm"
      onSubmit={submit}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isLoading}>
            {t('common:buttons.cancel')}
          </Button>
          <Button
            variant="primary"
            leftIcon={<Send />}
            onClick={submit}
            isLoading={isLoading}
          >
            {bulk
              ? t('sendReminderDialog.confirmBulk', { count })
              : t('sendReminderDialog.confirm')}
          </Button>
        </>
      }
    >
      <div>
        <label
          htmlFor="send-reminder-notes"
          className="block text-sm font-medium text-text-secondary mb-2"
        >
          {t('sendReminderDialog.notesLabel')}
        </label>
        <Textarea
          id="send-reminder-notes"
          rows={4}
          maxLength={1000}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder={t('sendReminderDialog.notesPlaceholder')}
        />
      </div>
    </ModalWrapper>
  );
};
