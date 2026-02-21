import { useState } from 'react';
import { X, Save } from 'lucide-react';
import {
  ExpenseResponse,
  CreateExpenseRequest,
  ExpenseCategory,
  formatExpenseCategory,
} from '@/types/expense';
import { MoneyInput } from '@/components/common/MoneyInput';
import { PropertySelector } from '@/components/common/PropertySelector';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

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
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateExpenseRequest>({
    propertyIdentifier:
      prefilledPropertyId || expense?.property.identifier || '',
    category: expense?.category || ExpenseCategory.MAINTENANCE,
    amount: expense?.amount || 0,
    currency: expense?.currency || defaultCurrency || 'EUR',
    expenseDate: expense?.expenseDate || '',
    description: expense?.description || '',
    notes: expense?.notes || '',
  });

  const [lastSyncedExpense, setLastSyncedExpense] = useState(expense);
  if (expense && expense !== lastSyncedExpense) {
    setLastSyncedExpense(expense);
    setFormData({
      propertyIdentifier: expense.property.identifier,
      category: expense.category,
      amount: expense.amount,
      currency: expense.currency,
      expenseDate: expense.expenseDate,
      description: expense.description,
      notes: expense.notes || '',
    });
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.propertyIdentifier)
      newErrors.propertyIdentifier = 'Property is required';
    if (formData.amount <= 0)
      newErrors.amount = 'Amount must be greater than 0';
    if (!formData.expenseDate)
      newErrors.expenseDate = 'Expense date is required';
    if (!formData.description || formData.description.trim().length === 0)
      newErrors.description = 'Description is required';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) return;
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
      {/* Property */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Property <span className="text-red-500">*</span>
        </label>
        <PropertySelector
          value={formData.propertyIdentifier || ''}
          onChange={(selected) =>
            setFormData({
              ...formData,
              propertyIdentifier: (selected as string) || '',
            })
          }
          disabled={isLoading}
        />
        {errors.propertyIdentifier && (
          <p className="mt-1 text-sm text-red-500">
            {errors.propertyIdentifier}
          </p>
        )}
      </div>

      {/* Category */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
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
          className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md"
          disabled={isLoading}
        >
          {Object.values(ExpenseCategory).map((cat) => (
            <option key={cat} value={cat}>
              {formatExpenseCategory(cat)}
            </option>
          ))}
        </select>
      </div>

      {/* Amount */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Amount <span className="text-red-500">*</span>
        </label>
        <MoneyInput
          value={formData.amount || undefined}
          onChange={(val) => setFormData({ ...formData, amount: val ?? 0 })}
          currency={formData.currency || defaultCurrency || 'EUR'}
          onCurrencyChange={(currency) => setFormData({ ...formData, currency })}
          disabled={isLoading}
          error={!!errors.amount}
        />
        {errors.amount && (
          <p className="mt-1 text-sm text-red-500">{errors.amount}</p>
        )}
      </div>

      {/* Expense Date */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Expense Date <span className="text-red-500">*</span>
        </label>
        <div className="flex gap-2">
          <input
            type="date"
            value={formData.expenseDate}
            onChange={(e) =>
              setFormData({ ...formData, expenseDate: e.target.value })
            }
            className={`flex-1 px-3 py-2 border rounded-md ${
              errors.expenseDate
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
                expenseDate: new Date().toISOString().split('T')[0],
              })
            }
            className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md transition-colors"
            disabled={isLoading}
          >
            Today
          </button>
        </div>
        {errors.expenseDate && (
          <p className="mt-1 text-sm text-red-500">{errors.expenseDate}</p>
        )}
      </div>

      {/* Description */}
      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Description <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          value={formData.description}
          onChange={(e) =>
            setFormData({ ...formData, description: e.target.value })
          }
          className={`w-full px-3 py-2 border rounded-md ${
            errors.description
              ? 'border-red-500'
              : 'border-[#c9cfd9] dark:border-[#3a3f54]'
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
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
          Notes
        </label>
        <RichTextEditor
          value={formData.notes || ''}
          onChange={(value) => setFormData({ ...formData, notes: value })}
          placeholder="Add any additional notes about this expense..."
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
          className="px-4 py-2 text-white bg-[#5c7cfa] rounded-md hover:bg-[#4c6ef5] flex items-center gap-2 disabled:opacity-50"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading ? 'Saving...' : expense ? 'Update' : 'Create'} Expense
        </button>
      </div>
    </form>
  );
};
