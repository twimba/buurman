import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Send } from 'lucide-react';
import { Button, ModalWrapper, Textarea } from '@buurman/ui';
import { ReminderTone } from '@/generated/models';

interface SendReminderDialogProps {
  open: boolean;
  /** Number of payments the reminder will be sent for. 1 = single payment wording. */
  count: number;
  isLoading?: boolean;
  onConfirm: (notes: string | undefined, tone: ReminderTone) => Promise<void> | void;
  onClose: () => void;
}

const TONES: ReminderTone[] = [ReminderTone.FRIENDLY, ReminderTone.FIRM, ReminderTone.FINAL];

export const SendReminderDialog = ({
  open,
  count,
  isLoading,
  onConfirm,
  onClose,
}: SendReminderDialogProps) => {
  const { t } = useTranslation('payments');
  const [notes, setNotes] = useState('');
  const [tone, setTone] = useState<ReminderTone>(ReminderTone.FRIENDLY);
  const bulk = count > 1;

  const submit = () => {
    void onConfirm(notes.trim() || undefined, tone);
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
      <div className="space-y-4">
        <fieldset>
          <legend className="block text-sm font-medium text-text-secondary mb-2">
            {t('sendReminderDialog.toneLabel')}
          </legend>
          <div className="grid grid-cols-3 gap-2" role="radiogroup">
            {TONES.map((option) => {
              const active = tone === option;
              return (
                <button
                  key={option}
                  type="button"
                  role="radio"
                  aria-checked={active}
                  onClick={() => setTone(option)}
                  className={`rounded-md border px-3 py-2 text-sm text-left focus-ring ${
                    active
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30 text-text-primary'
                      : 'border-border-default text-text-secondary hover:border-border-strong'
                  }`}
                >
                  <span className="block font-medium">
                    {t(`sendReminderDialog.tone.${option}.label`)}
                  </span>
                </button>
              );
            })}
          </div>
          <p className="text-xs text-text-muted mt-2">
            {t(`sendReminderDialog.tone.${tone}.help`)}
          </p>
        </fieldset>
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
      </div>
    </ModalWrapper>
  );
};
