import { useState, useEffect } from 'react';
import { X, Save } from 'lucide-react';
import {
  ExpenseResponse,
  CreateExpenseRequest,
  ExpenseCategory,
} from '@/types/expense';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { PropertySelector } from '@/components/common/PropertySelector';

interface ExpenseFormProps {
  expense?: ExpenseResponse;
  onSubmit: (data: CreateExpenseRequest) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  prefilledPropertyId?: string;
}

export const ExpenseForm = ({
  expense,
  onSubmit,
  onCancel,
  isLoading,
  prefilledPropertyId,
}: ExpenseFormProps) => {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateExpenseRequest>({
    propertyId: prefilledPropertyId || expense?.property.id || '',
    category: expense?.category || ExpenseCategory.MAINTENANCE,
    amount: expense?.amount || 0,
    currency: expense?.currency || 'EUR',
    expenseDate: expense?.expenseDate || '',
    description: expense?.description || '',
    notes: expense?.notes || '',
  });

  useEffect(() => {
    if (expense) {
      setFormData({
        propertyId: expense.property.id,
        category: expense.category,
        amount: expense.amount,
        currency: expense.currency,
        expenseDate: expense.expenseDate,
        description: expense.description,
        notes: expense.notes || '',
      });
    }
  }, [expense]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.propertyId) newErrors.propertyId = 'Property is required';
    if (formData.amount <= 0)
      newErrors.amount = 'Amount must be greater than 0';
    if (!formData.expenseDate)
      newErrors.expenseDate = 'Expense date is required';
    if (!formData.description || formData.description.trim().length === 0)
      newErrors.description = 'Description is required';

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
      {/* Property */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Property <span className="text-red-500">*</span>
        </label>
        <PropertySelector
          value={formData.propertyId || ''}
          onChange={(selected) =>
            setFormData({ ...formData, propertyId: (selected as string) || '' })
          }
          disabled={isLoading}
        />
        {errors.propertyId && (
          <p className="mt-1 text-sm text-red-500">{errors.propertyId}</p>
        )}
      </div>

      {/* Category */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Category <span className="text-red-500">*</span>
        </label>
        <select
          value={formData.category}
          onChange={(e) =>
            setFormData({
              ...formData,
              category: e.target.value as ExpenseCategory,
            })
          }
          className="w-full px-3 py-2 border border-gray-300 rounded-md"
          disabled={isLoading}
        >
          {Object.values(ExpenseCategory).map((cat) => (
            <option key={cat} value={cat}>
              {cat.replace('_', ' ')}
            </option>
          ))}
        </select>
      </div>

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

      {/* Expense Date */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Expense Date <span className="text-red-500">*</span>
        </label>
        <input
          type="date"
          value={formData.expenseDate}
          onChange={(e) =>
            setFormData({ ...formData, expenseDate: e.target.value })
          }
          className={`w-full px-3 py-2 border rounded-md ${
            errors.expenseDate ? 'border-red-500' : 'border-gray-300'
          }`}
          disabled={isLoading}
        />
        {errors.expenseDate && (
          <p className="mt-1 text-sm text-red-500">{errors.expenseDate}</p>
        )}
      </div>

      {/* Description */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Description <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          value={formData.description}
          onChange={(e) =>
            setFormData({ ...formData, description: e.target.value })
          }
          className={`w-full px-3 py-2 border rounded-md ${
            errors.description ? 'border-red-500' : 'border-gray-300'
          }`}
          disabled={isLoading}
          placeholder="e.g., Plumbing repair in bathroom"
        />
        {errors.description && (
          <p className="mt-1 text-sm text-red-500">{errors.description}</p>
        )}
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Notes
        </label>
        <textarea
          value={formData.notes}
          onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
          rows={3}
          className="w-full px-3 py-2 border border-gray-300 rounded-md"
          disabled={isLoading}
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
          {isLoading ? 'Saving...' : expense ? 'Update' : 'Create'} Expense
        </button>
      </div>
    </form>
  );
};
