import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, ModalWrapper } from '@buurman/ui';
import type { ContactCreditResponse } from '@/types/payment';

interface ApplyCreditDialogProps {
  open: boolean;
  credits: ContactCreditResponse[];
  balance: number;
  currency: string;
  formatMoney: (value: number, currency: string) => string;
  isLoading?: boolean;
  onConfirm: (
    creditIdentifier: string,
    amount?: number
  ) => Promise<void> | void;
  onClose: () => void;
}

/** Pick one of the tenant's open credits and how much of it to put against this payment. */
export const ApplyCreditDialog = ({
  open,
  credits,
  balance,
  currency,
  formatMoney,
  isLoading,
  onConfirm,
  onClose,
}: ApplyCreditDialogProps) => {
  const { t } = useTranslation('payments');
  const usable = useMemo(
    () =>
      credits.filter((c) => c.remainingAmount > 0 && c.currency === currency),
    [credits, currency]
  );
  const [creditId, setCreditId] = useState(usable[0]?.identifier ?? '');
  const selected = usable.find((c) => c.identifier === creditId) ?? usable[0];
  const max = selected ? Math.min(selected.remainingAmount, balance) : 0;
  const [amount, setAmount] = useState<string>('');
  const parsed = amount === '' ? max : Number.parseFloat(amount);
  const valid = !!selected && parsed > 0 && parsed <= max + 1e-9;

  const submit = () => {
    if (!valid || !selected) {
      return;
    }
    void onConfirm(selected.identifier, amount === '' ? undefined : parsed);
  };

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={t('applyCreditDialog.title')}
      subtitle={t('applyCreditDialog.message', {
        balance: formatMoney(balance, currency),
      })}
      size="sm"
      onSubmit={submit}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isLoading}>
            {t('common:buttons.cancel')}
          </Button>
          <Button
            variant="primary"
            onClick={submit}
            isLoading={isLoading}
            disabled={!valid}
          >
            {t('applyCreditDialog.confirm', {
              amount: formatMoney(valid ? parsed : 0, currency),
            })}
          </Button>
        </>
      }
    >
      {usable.length === 0 ? (
        <p className="text-sm text-text-secondary">
          {t('applyCreditDialog.none')}
        </p>
      ) : (
        <div className="space-y-4">
          <div>
            <label
              htmlFor="apply-credit-select"
              className="block text-sm font-medium text-text-secondary mb-2"
            >
              {t('applyCreditDialog.credit')}
            </label>
            <select
              id="apply-credit-select"
              value={selected?.identifier ?? ''}
              onChange={(e) => {
                setCreditId(e.target.value);
                setAmount('');
              }}
              className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary min-h-touch"
            >
              {usable.map((c) => (
                <option key={c.identifier} value={c.identifier}>
                  {formatMoney(c.remainingAmount, c.currency)} ·{' '}
                  {t(`credits.sources.${c.source}`)}
                  {c.reason ? ` · ${c.reason}` : ''}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label
              htmlFor="apply-credit-amount"
              className="block text-sm font-medium text-text-secondary mb-2"
            >
              {t('applyCreditDialog.amount', {
                max: formatMoney(max, currency),
              })}
            </label>
            <input
              id="apply-credit-amount"
              type="number"
              step="0.01"
              min="0.01"
              max={max}
              value={amount}
              placeholder={max.toFixed(2)}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary min-h-touch"
            />
          </div>
        </div>
      )}
    </ModalWrapper>
  );
};
