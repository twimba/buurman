import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, ModalWrapper, Textarea } from '@buurman/ui';

interface ReasonDialogProps {
  open: boolean;
  title: string;
  message?: ReactNode;
  reasonLabel: string;
  reasonPlaceholder?: string;
  confirmLabel: string;
  variant?: 'primary' | 'danger' | 'success';
  /** When set, a date field is shown and passed back to onConfirm. */
  dateLabel?: string;
  isLoading?: boolean;
  onConfirm: (reason: string, date?: string) => Promise<void> | void;
  onClose: () => void;
}

/** Confirmation dialog that requires a written reason (cancel, waive, write-off). */
export const ReasonDialog = ({
  open,
  title,
  message,
  reasonLabel,
  reasonPlaceholder,
  confirmLabel,
  variant = 'primary',
  dateLabel,
  isLoading,
  onConfirm,
  onClose,
}: ReasonDialogProps) => {
  const { t } = useTranslation('common');
  const [reason, setReason] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const valid = reason.trim().length > 0;

  const submit = () => {
    if (!valid) {
      return;
    }
    void onConfirm(reason.trim(), dateLabel ? date : undefined);
  };

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={title}
      size="sm"
      onSubmit={submit}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isLoading}>
            {t('buttons.cancel')}
          </Button>
          <Button
            variant={variant}
            onClick={submit}
            isLoading={isLoading}
            disabled={!valid}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {message && <p className="text-sm text-text-secondary">{message}</p>}
        {dateLabel && (
          <div>
            <label
              htmlFor="reason-dialog-date"
              className="block text-sm font-medium text-text-secondary mb-2"
            >
              {dateLabel}
            </label>
            <input
              id="reason-dialog-date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full px-3 py-2 border border-border-strong rounded-md bg-surface-card text-text-primary min-h-touch"
            />
          </div>
        )}
        <div>
          <label
            htmlFor="reason-dialog-reason"
            className="block text-sm font-medium text-text-secondary mb-2"
          >
            {reasonLabel} <span className="text-error-text">*</span>
          </label>
          <Textarea
            id="reason-dialog-reason"
            rows={3}
            maxLength={1000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={reasonPlaceholder}
          />
        </div>
      </div>
    </ModalWrapper>
  );
};
