import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CheckCircle } from 'lucide-react';
import { Button, ModalWrapper, Textarea } from '@buurman/ui';

interface BulkMarkPaidDialogProps {
  open: boolean;
  count: number;
  isLoading?: boolean;
  onConfirm: (paymentDate: string, notes?: string) => Promise<void> | void;
  onClose: () => void;
}

export const BulkMarkPaidDialog = ({
  open,
  count,
  isLoading,
  onConfirm,
  onClose,
}: BulkMarkPaidDialogProps) => {
  const { t } = useTranslation('payments');
  const [paymentDate, setPaymentDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [notes, setNotes] = useState('');

  const submit = () => {
    if (!paymentDate) {
      return;
    }
    void onConfirm(paymentDate, notes.trim() || undefined);
  };

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={t('bulkMarkPaidDialog.title', { count })}
      subtitle={t('bulkMarkPaidDialog.message')}
      size="sm"
      onSubmit={submit}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isLoading}>
            {t('common:buttons.cancel')}
          </Button>
          <Button
            variant="success"
            leftIcon={<CheckCircle />}
            onClick={submit}
            isLoading={isLoading}
            disabled={!paymentDate}
          >
            {t('bulkMarkPaidDialog.confirm', { count })}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label
            htmlFor="bulk-mark-paid-date"
            className="block text-sm font-medium text-text-secondary mb-2"
          >
            {t('markPaidDialog.paymentDate')}{' '}
            <span className="text-error-text">*</span>
          </label>
          <input
            id="bulk-mark-paid-date"
            type="date"
            value={paymentDate}
            onChange={(e) => setPaymentDate(e.target.value)}
            className="w-full px-3 py-2 border border-border-strong rounded-md bg-surface-card text-text-primary min-h-touch"
          />
        </div>
        <div>
          <label
            htmlFor="bulk-mark-paid-notes"
            className="block text-sm font-medium text-text-secondary mb-2"
          >
            {t('markPaidDialog.notesOptional')}
          </label>
          <Textarea
            id="bulk-mark-paid-notes"
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder={t('markPaidDialog.notesPlaceholder')}
          />
        </div>
      </div>
    </ModalWrapper>
  );
};
