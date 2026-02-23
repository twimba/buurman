import { useState, useMemo, useEffect } from 'react';
import { X, Calendar, Save, Info } from 'lucide-react';
import { PaymentResponse, CreatePaymentRequest } from '@/types/payment';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useCurrencies, getFractionalDigits } from '@/hooks/useCurrencies';
import { useRentPeriods } from '@/hooks/useRentPeriodHooks';
import { getCurrencySymbol } from '@/utils/currencies';

interface PaymentFormProps {
  payment?: PaymentResponse;
  onSubmit: (data: CreatePaymentRequest) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  contractIdentifier: string;
  resetKey?: number;
  continueAdding?: boolean;
  onContinueAddingChange?: (value: boolean) => void;
}

export const PaymentForm = ({
  payment,
  onSubmit,
  onCancel,
  isLoading,
  contractIdentifier,
  resetKey,
  continueAdding,
  onContinueAddingChange,
}: PaymentFormProps) => {
  const { defaultCurrency } = useTeamDefaults();
  const { data: currencies } = useCurrencies();
  const { data: rentPeriods } = useRentPeriods(contractIdentifier || undefined);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<
    CreatePaymentRequest & { paymentDate?: string }
  >({
    contractIdentifier: contractIdentifier,
    amount: payment?.amount || 0,
    currency: payment?.currency || defaultCurrency || '',
    dueDate: payment?.dueDate || new Date().toISOString().split('T')[0],
    notes: payment?.notes || '',
    paymentDate: payment?.paymentDate || '',
  });

  const [lastSyncedPayment, setLastSyncedPayment] = useState(payment);
  if (payment && payment !== lastSyncedPayment) {
    setLastSyncedPayment(payment);
    setFormData({
      contractIdentifier: payment.contract.identifier,
      amount: payment.amount,
      currency: payment.currency,
      dueDate: payment.dueDate,
      notes: payment.notes || '',
      paymentDate: payment.paymentDate || '',
    });
  }

  useEffect(() => {
    if (!defaultCurrency) {
      return;
    }
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) =>
      prev.currency ? prev : { ...prev, currency: defaultCurrency }
    );
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [defaultCurrency]);

  useEffect(() => {
    if (resetKey === undefined || resetKey === 0 || payment) {
      return;
    }
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) => ({
      ...prev,
      amount: 0,
      dueDate: new Date().toISOString().split('T')[0],
      notes: '',
    }));
    setErrors({});
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [resetKey, payment]);

  const currency = formData.currency || defaultCurrency || '';
  const fractionalDigits = getFractionalDigits(currencies, currency);

  const dueDate = formData.dueDate;

  // Determine which rent period the due date falls into
  const rentPeriodForDate = useMemo(() => {
    if (!rentPeriods || !dueDate) {
      return null;
    }
    return (
      rentPeriods.find((rp) => {
        return (
          dueDate >= rp.effectiveFrom &&
          (!rp.effectiveTo || dueDate <= rp.effectiveTo)
        );
      }) ?? null
    );
  }, [rentPeriods, dueDate]);

  // Current rent period (no end date or end date >= today)
  const currentRentPeriod = useMemo(() => {
    if (!rentPeriods) {
      return null;
    }
    const today = new Date().toISOString().split('T')[0];
    return (
      rentPeriods.find((rp) => {
        return (
          rp.effectiveFrom <= today &&
          (!rp.effectiveTo || rp.effectiveTo >= today)
        );
      }) ?? null
    );
  }, [rentPeriods]);

  // Show notice when the due date's rent period differs from the current one,
  // or when the due date falls in a known period but there is no current period
  const showRentNotice =
    !!rentPeriodForDate &&
    (!currentRentPeriod ||
      rentPeriodForDate.identifier !== currentRentPeriod.identifier);

  const handleCurrencyChange = (newCurrency: string) => {
    setFormData({ ...formData, currency: newCurrency });
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (formData.amount <= 0) {
      newErrors.amount = 'Amount must be greater than 0';
    }
    if (!formData.dueDate) {
      newErrors.dueDate = 'Due date is required';
    }
    if (formData.amount > 0 && !currency.trim()) {
      newErrors.currency = 'Currency is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }
    await onSubmit(formData);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-6"
    >
      {/* Amount */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Amount <span className="text-red-500">*</span>
        </label>
        <MoneyInput
          value={formData.amount || undefined}
          onChange={(val) => setFormData({ ...formData, amount: val ?? 0 })}
          currency={currency}
          onCurrencyChange={handleCurrencyChange}
          disabled={isLoading}
          error={!!errors.amount || !!errors.currency}
        />
        {(errors.amount || errors.currency) && (
          <p className="mt-1 text-sm text-red-500">
            {errors.amount || errors.currency}
          </p>
        )}
      </div>

      {/* Due Date */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Due Date <span className="text-red-500">*</span>
        </label>
        <div className="flex gap-2">
          <input
            type="date"
            value={formData.dueDate}
            onChange={(e) =>
              setFormData({ ...formData, dueDate: e.target.value })
            }
            className={`flex-1 px-3 py-2 border rounded-md ${
              errors.dueDate
                ? 'border-red-500'
                : 'border-[#c9cfd9] dark:border-[#3a3f54]'
            }`}
            disabled={isLoading}
          />
          <button
            type="button"
            onClick={() =>
              setFormData({
                ...formData,
                dueDate: new Date().toISOString().split('T')[0],
              })
            }
            className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:bg-[#1e2130] border border-[#c9cfd9] rounded-md transition-colors"
            disabled={isLoading}
          >
            Today
          </button>
        </div>
        {errors.dueDate && (
          <p className="mt-1 text-sm text-red-500">{errors.dueDate}</p>
        )}
        {showRentNotice && rentPeriodForDate && (
          <div className="mt-2 flex items-start gap-2 p-2 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded text-xs text-amber-700 dark:text-amber-300">
            <Info className="h-3.5 w-3.5 flex-shrink-0 mt-0.5" />
            <span>
              This date falls in a different rent period (
              {getCurrencySymbol(currency)}{' '}
              {rentPeriodForDate.rentAmount.toFixed(fractionalDigits)}/mo from{' '}
              {rentPeriodForDate.effectiveFrom}
              {rentPeriodForDate.effectiveTo
                ? ` to ${rentPeriodForDate.effectiveTo}`
                : ''}
              ).
              {currentRentPeriod && (
                <>
                  {' '}
                  Current rent is {getCurrencySymbol(currency)}{' '}
                  {currentRentPeriod.rentAmount.toFixed(fractionalDigits)}/mo.
                </>
              )}
            </span>
          </div>
        )}
      </div>

      {/* Payment Date - only show when editing */}
      {payment && (
        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
            Payment Date
          </label>
          <div className="flex gap-2">
            <input
              type="date"
              value={formData.paymentDate || ''}
              onChange={(e) =>
                setFormData({ ...formData, paymentDate: e.target.value })
              }
              className="flex-1 px-3 py-2 border border-[#c9cfd9] rounded-md"
              disabled={isLoading}
            />
            {formData.paymentDate && (
              <button
                type="button"
                onClick={() => setFormData({ ...formData, paymentDate: '' })}
                className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:bg-[#1e2130] border border-[#c9cfd9] rounded-md transition-colors"
                disabled={isLoading}
              >
                Clear
              </button>
            )}
          </div>
        </div>
      )}

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Notes
        </label>
        <RichTextEditor
          value={formData.notes || ''}
          onChange={(value) => setFormData({ ...formData, notes: value })}
          placeholder="Add any additional notes about this payment..."
          readOnly={isLoading}
          onSubmit={submitForm}
        />
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        {onContinueAddingChange && (
          <label className="flex items-center gap-2 cursor-pointer select-none mr-auto">
            <input
              type="checkbox"
              checked={continueAdding ?? false}
              onChange={(e) => onContinueAddingChange(e.target.checked)}
              className="h-4 w-4 rounded border-[#c9cfd9] dark:border-[#3a3f54] text-[#5c7cfa] focus:ring-[#5c7cfa]"
            />
            <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
              Continue adding more
            </span>
          </label>
        )}
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 text-white bg-[#5c7cfa] rounded-md hover:bg-[#4c6ef5] flex items-center gap-1.5 disabled:opacity-50"
          disabled={isLoading}
        >
          {payment ? (
            <>
              <Save className="h-4 w-4" />
              {isLoading ? 'Saving...' : 'Update Payment'}
            </>
          ) : (
            <>
              <Calendar className="h-4 w-4" />
              {isLoading ? 'Saving...' : 'Schedule Payment'}
            </>
          )}
        </button>
      </div>
    </form>
  );
};
