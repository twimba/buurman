import { useState, useEffect } from 'react';
import { X, CalendarCheck } from 'lucide-react';
import { CreatePaymentRequest } from '@/types/payment';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

interface RegisterPaymentFormProps {
  onSubmit: (data: CreatePaymentRequest) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  contractIdentifier: string;
  resetKey?: number;
}

export const RegisterPaymentForm = ({
  onSubmit,
  onCancel,
  isLoading,
  contractIdentifier,
  resetKey,
}: RegisterPaymentFormProps) => {
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState({
    amount: 0,
    currency: defaultCurrency || '',
    paymentDate: new Date().toISOString().split('T')[0],
    notes: '',
  });

  useEffect(() => {
    if (!defaultCurrency) return;
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) =>
      prev.currency ? prev : { ...prev, currency: defaultCurrency }
    );
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [defaultCurrency]);

  useEffect(() => {
    if (resetKey === undefined || resetKey === 0) return;
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) => ({
      ...prev,
      amount: 0,
      paymentDate: new Date().toISOString().split('T')[0],
      notes: '',
    }));
    setErrors({});
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [resetKey]);

  const currency = formData.currency || defaultCurrency || '';

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (formData.amount <= 0)
      newErrors.amount = 'Amount must be greater than 0';
    if (formData.amount > 0 && !currency.trim())
      newErrors.currency = 'Currency is required';
    if (!formData.paymentDate)
      newErrors.paymentDate = 'Payment date is required';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) return;
    await onSubmit({
      contractIdentifier,
      amount: formData.amount,
      currency: formData.currency,
      dueDate: formData.paymentDate,
      notes: formData.notes || undefined,
      markAsPaid: true,
    });
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
          onCurrencyChange={(c) => setFormData({ ...formData, currency: c })}
          disabled={isLoading}
          error={!!errors.amount || !!errors.currency}
        />
        {(errors.amount || errors.currency) && (
          <p className="mt-1 text-sm text-red-500">
            {errors.amount || errors.currency}
          </p>
        )}
      </div>

      {/* Payment Date */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Payment Date <span className="text-red-500">*</span>
        </label>
        <div className="flex gap-2">
          <input
            type="date"
            value={formData.paymentDate}
            onChange={(e) =>
              setFormData({ ...formData, paymentDate: e.target.value })
            }
            className={`flex-1 px-3 py-2 border rounded-md ${
              errors.paymentDate
                ? 'border-red-500'
                : 'border-[#c9cfd9] dark:border-[#3a3f54]'
            } bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]`}
            disabled={isLoading}
          />
          <button
            type="button"
            onClick={() =>
              setFormData({
                ...formData,
                paymentDate: new Date().toISOString().split('T')[0],
              })
            }
            className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md transition-colors text-[#3d4463] dark:text-[#c4c8db]"
            disabled={isLoading}
          >
            Today
          </button>
        </div>
        {errors.paymentDate && (
          <p className="mt-1 text-sm text-red-500">{errors.paymentDate}</p>
        )}
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Notes
        </label>
        <RichTextEditor
          value={formData.notes}
          onChange={(value) => setFormData({ ...formData, notes: value })}
          placeholder="Add any additional notes about this payment..."
          readOnly={isLoading}
          onSubmit={submitForm}
        />
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
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
          <CalendarCheck className="h-4 w-4" />
          {isLoading ? 'Saving...' : 'Register Payment'}
        </button>
      </div>
    </form>
  );
};
