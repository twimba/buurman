import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, ModalWrapper, Textarea } from '@buurman/ui';
import { useCreatePaymentPlan } from '@/hooks/useContractHooks';
import type { CreatePaymentPlanRequest } from '@/types/contract';

interface CreatePaymentPlanDialogProps {
  open: boolean;
  contractIdentifier: string;
  paymentIdentifiers: string[];
  totalOutstanding: number;
  currency: string;
  formatMoney: (value: number, currency: string) => string;
  onCreated: () => void;
  onClose: () => void;
}

type Frequency = NonNullable<CreatePaymentPlanRequest['frequency']>;

/** Turn a set of open payments on one contract into an instalment plan. */
export const CreatePaymentPlanDialog = ({
  open,
  contractIdentifier,
  paymentIdentifiers,
  totalOutstanding,
  currency,
  formatMoney,
  onCreated,
  onClose,
}: CreatePaymentPlanDialogProps) => {
  const { t } = useTranslation('payments');
  const create = useCreatePaymentPlan(
    contractIdentifier,
    t('paymentPlanDialog.created')
  );
  const [count, setCount] = useState(3);
  const [startDate, setStartDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [frequency, setFrequency] = useState<Frequency>('MONTHLY');
  const [notes, setNotes] = useState('');
  const [pauseReminders, setPauseReminders] = useState(true);
  const perInstalment = count > 0 ? totalOutstanding / count : 0;
  const valid = count >= 1 && count <= 36 && !!startDate;

  const submit = async () => {
    if (!valid) {
      return;
    }
    await create.mutateAsync({
      paymentIdentifiers,
      instalmentCount: count,
      startDate,
      frequency,
      notes: notes.trim() || undefined,
      pauseReminders,
    });
    onCreated();
  };

  const inputClass =
    'w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary min-h-touch';

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={t('paymentPlanDialog.title')}
      subtitle={t('paymentPlanDialog.message', {
        count: paymentIdentifiers.length,
        total: formatMoney(totalOutstanding, currency),
      })}
      size="sm"
      onSubmit={submit}
      footer={
        <>
          <Button
            variant="secondary"
            onClick={onClose}
            disabled={create.isPending}
          >
            {t('common:buttons.cancel')}
          </Button>
          <Button
            variant="primary"
            onClick={submit}
            isLoading={create.isPending}
            disabled={!valid}
          >
            {t('paymentPlanDialog.confirm')}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label
              htmlFor="plan-count"
              className="block text-sm font-medium text-text-secondary mb-1"
            >
              {t('paymentPlanDialog.instalments')}
            </label>
            <input
              id="plan-count"
              type="number"
              min={1}
              max={36}
              value={count}
              onChange={(e) =>
                setCount(Number.parseInt(e.target.value || '1', 10))
              }
              className={inputClass}
            />
          </div>
          <div>
            <label
              htmlFor="plan-frequency"
              className="block text-sm font-medium text-text-secondary mb-1"
            >
              {t('paymentPlanDialog.frequency')}
            </label>
            <select
              id="plan-frequency"
              value={frequency}
              onChange={(e) => setFrequency(e.target.value as Frequency)}
              className={inputClass}
            >
              <option value="WEEKLY">
                {t('paymentPlanDialog.frequencies.WEEKLY')}
              </option>
              <option value="BIWEEKLY">
                {t('paymentPlanDialog.frequencies.BIWEEKLY')}
              </option>
              <option value="MONTHLY">
                {t('paymentPlanDialog.frequencies.MONTHLY')}
              </option>
            </select>
          </div>
        </div>
        <div>
          <label
            htmlFor="plan-start"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('paymentPlanDialog.startDate')}
          </label>
          <input
            id="plan-start"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <p className="text-sm text-text-secondary">
          {t('paymentPlanDialog.preview', {
            count,
            amount: formatMoney(perInstalment, currency),
          })}
        </p>
        <label className="flex items-start gap-2 text-sm text-text-primary">
          <input
            type="checkbox"
            checked={pauseReminders}
            onChange={(e) => setPauseReminders(e.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-border-strong text-primary-500 focus-ring"
          />
          <span>
            {t('paymentPlanDialog.pauseReminders')}
            <span className="block text-text-secondary">
              {t('paymentPlanDialog.pauseRemindersHelp')}
            </span>
          </span>
        </label>
        <div>
          <label
            htmlFor="plan-notes"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('paymentPlanDialog.notes')}
          </label>
          <Textarea
            id="plan-notes"
            rows={2}
            maxLength={2000}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>
      </div>
    </ModalWrapper>
  );
};
