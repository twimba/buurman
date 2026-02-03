import { useState, useEffect } from 'react';
import { X, Save } from 'lucide-react';
import { PaymentResponse, CreatePaymentRequest } from '@/types/payment';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { RichTextEditor } from '@/components/common/RichTextEditor';

interface PaymentFormProps {
  payment?: PaymentResponse;
  onSubmit: (data: CreatePaymentRequest) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  contractId: string;
}

export const PaymentForm = ({
  payment,
  onSubmit,
  onCancel,
  isLoading,
  contractId,
}: PaymentFormProps) => {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreatePaymentRequest>({
    contractId: contractId,
    amount: payment?.amount || 0,
    currency: payment?.currency || 'EUR',
    dueDate: payment?.dueDate || '',
    notes: payment?.notes || '',
  });

  useEffect(() => {
    if (payment) {
      setFormData({
        contractId: payment.contract.id,
        amount: payment.amount,
        currency: payment.currency,
        dueDate: payment.dueDate,
        notes: payment.notes || '',
      });
    }
  }, [payment]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (formData.amount <= 0)
      newErrors.amount = 'Amount must be greater than 0';
    if (!formData.dueDate) newErrors.dueDate = 'Due date is required';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    await onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Amount */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Amount <span className="text-red-500">*</span>
        </label>
        <input
          type="number"
          step="0.01"
          value={formData.amount}
          onChange={(e) =>
            setFormData({ ...formData, amount: parseFloat(e.target.value) })
          }
          className={`w-full px-3 py-2 border rounded-md ${
            errors.amount ? 'border-red-500' : 'border-gray-300'
          }`}
          disabled={isLoading}
        />
        {errors.amount && (
          <p className="mt-1 text-sm text-red-500">{errors.amount}</p>
        )}
      </div>

      {/* Currency */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Currency
        </label>
        <CurrencySelector
          value={formData.currency || 'EUR'}
          onChange={(currency) => setFormData({ ...formData, currency })}
          disabled={isLoading}
        />
      </div>

      {/* Due Date */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
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
              errors.dueDate ? 'border-red-500' : 'border-gray-300'
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
            className="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 border border-gray-300 rounded-md transition-colors"
            disabled={isLoading}
          >
            Today
          </button>
        </div>
        {errors.dueDate && (
          <p className="mt-1 text-sm text-red-500">{errors.dueDate}</p>
        )}
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Notes
        </label>
        <RichTextEditor
          value={formData.notes || ''}
          onChange={(value) => setFormData({ ...formData, notes: value })}
          placeholder="Add any additional notes about this payment..."
          readOnly={isLoading}
        />
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 flex items-center gap-2 disabled:opacity-50"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading ? 'Saving...' : payment ? 'Update' : 'Create'} Payment
        </button>
      </div>
    </form>
  );
};
