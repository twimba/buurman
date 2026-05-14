import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Save } from 'lucide-react';
import {
  ExpenseResponse,
  CreateExpenseRequest,
  ExpenseCategory,
} from '@/types/expense';
import { MoneyInput } from '@/components/common/MoneyInput';
import { PropertySelector } from '@/components/common/PropertySelector';
import { ContactSelector } from '@/components/common/ContactSelector';
import { RichTextEditor } from '@buurman/ui';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

interface ExpenseFormProps {
  expense?: ExpenseResponse;
  onSubmit: (data: CreateExpenseRequest) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  prefilledPropertyId?: string;
  resetKey?: number;
  continueAdding?: boolean;
  onContinueAddingChange?: (value: boolean) => void;
}

export const ExpenseForm = ({
  expense,
  onSubmit,
  onCancel,
  isLoading,
  prefilledPropertyId,
  resetKey,
  continueAdding,
  onContinueAddingChange,
}: ExpenseFormProps) => {
  const { t } = useTranslation('expenses');
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateExpenseRequest>({
    propertyIdentifier:
      prefilledPropertyId || expense?.property.identifier || '',
    contactIdentifier: expense?.contact?.identifier,
    category: expense?.category ?? ExpenseCategory.MAINTENANCE,
    amount: expense?.amount ?? 0,
    currency: expense?.currency || defaultCurrency || '',
    expenseDate: expense?.expenseDate ?? '',
    description: expense?.description ?? '',
    notes: expense?.notes ?? '',
  });

  const [lastSyncedExpense, setLastSyncedExpense] = useState(expense);
  if (expense && expense !== lastSyncedExpense) {
    setLastSyncedExpense(expense);
    setFormData({
      propertyIdentifier: expense.property.identifier,
      contactIdentifier: expense.contact?.identifier,
      category: expense.category,
      amount: expense.amount,
      currency: expense.currency,
      expenseDate: expense.expenseDate,
      description: expense.description,
      notes: expense.notes ?? '',
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
    if (resetKey === undefined || resetKey === 0 || expense) {
      return;
    }
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) => ({
      ...prev,
      contactIdentifier: undefined,
      amount: 0,
      expenseDate: '',
      description: '',
      notes: '',
    }));
    setErrors({});
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [resetKey, expense]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.propertyIdentifier) {
      newErrors.propertyIdentifier = t('validation.propertyRequired');
    }
    if (formData.amount <= 0) {
      newErrors.amount = t('validation.amountRequired');
    }
    if (
      formData.amount > 0 &&
      !(formData.currency || defaultCurrency || '').trim()
    )
      newErrors.currency = t('validation.currencyRequired');
    if (!formData.expenseDate) {
      newErrors.expenseDate = t('validation.expenseDateRequired');
    }
    if (!formData.description || formData.description.trim().length === 0) {
      newErrors.description = t('validation.descriptionRequired');
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
      {/* Property */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.property')} <span className="text-error-text">*</span>
        </label>
        <PropertySelector
          value={formData.propertyIdentifier ?? ''}
          onChange={(selected) =>
            setFormData({
              ...formData,
              propertyIdentifier: (selected as string) ?? '',
            })
          }
          disabled={isLoading}
        />
        {errors.propertyIdentifier && (
          <p className="mt-1 text-sm text-error-text">
            {errors.propertyIdentifier}
          </p>
        )}
      </div>

      {/* Category */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.category')} <span className="text-error-text">*</span>
        </label>
        <select
          value={formData.category}
          onChange={(e) =>
            setFormData({
              ...formData,
              category: e.target.value as ExpenseCategory,
            })
          }
          className="w-full px-3 py-2 border border-border-strong rounded-md"
          disabled={isLoading}
        >
          {Object.values(ExpenseCategory).map((cat) => (
            <option key={cat} value={cat}>
              {t(`category.${cat}`)}
            </option>
          ))}
        </select>
      </div>

      {/* Contact (optional) */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.contact')}
        </label>
        <div className="flex gap-2 items-center">
          <div className="flex-1">
            <ContactSelector
              value={formData.contactIdentifier}
              onChange={(selected) =>
                setFormData({
                  ...formData,
                  contactIdentifier: selected || undefined,
                })
              }
              disabled={isLoading}
            />
          </div>
          {formData.contactIdentifier && (
            <button
              type="button"
              onClick={() =>
                setFormData({ ...formData, contactIdentifier: undefined })
              }
              className="px-2 py-2 text-sm text-text-secondary hover:text-error-text border border-border-strong rounded-md hover:bg-surface-inset transition-colors"
              disabled={isLoading}
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {/* Amount */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.amount')} <span className="text-error-text">*</span>
        </label>
        <MoneyInput
          value={formData.amount ?? undefined}
          onChange={(val) => setFormData({ ...formData, amount: val ?? 0 })}
          currency={formData.currency || defaultCurrency || ''}
          disabled={isLoading}
          error={!!errors.amount || !!errors.currency}
        />
        {(errors.amount || errors.currency) && (
          <p className="mt-1 text-sm text-error-text">
            {errors.amount || errors.currency}
          </p>
        )}
      </div>

      {/* Expense Date */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.expenseDate')} <span className="text-error-text">*</span>
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
                ? 'border-error-border'
                : 'border-border-strong'
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
            className="px-3 py-2 text-sm bg-surface-inset hover:bg-surface-raised border border-border-strong rounded-md transition-colors"
            disabled={isLoading}
          >
            {t('form.today')}
          </button>
        </div>
        {errors.expenseDate && (
          <p className="mt-1 text-sm text-error-text">{errors.expenseDate}</p>
        )}
      </div>

      {/* Description */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.description')} <span className="text-error-text">*</span>
        </label>
        <input
          type="text"
          value={formData.description}
          onChange={(e) =>
            setFormData({ ...formData, description: e.target.value })
          }
          className={`w-full px-3 py-2 border rounded-md ${
            errors.description ? 'border-error-border' : 'border-border-strong'
          }`}
          disabled={isLoading}
          placeholder={t('form.descriptionPlaceholder')}
        />
        {errors.description && (
          <p className="mt-1 text-sm text-error-text">{errors.description}</p>
        )}
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.notes')}
        </label>
        <RichTextEditor
          value={formData.notes ?? ''}
          onChange={(value) => setFormData({ ...formData, notes: value })}
          placeholder={t('form.notesPlaceholder')}
          readOnly={isLoading}
          onSubmit={submitForm}
        />
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3 pt-4 border-t border-border-default">
        {onContinueAddingChange && (
          <label className="flex items-center gap-2 cursor-pointer select-none mr-auto">
            <input
              type="checkbox"
              checked={continueAdding ?? false}
              onChange={(e) => onContinueAddingChange(e.target.checked)}
              className="h-4 w-4 rounded border-border-strong text-primary-500 focus:ring-primary-500"
            />
            <span className="text-sm text-text-secondary">
              {t('form.continueAdding')}
            </span>
          </label>
        )}
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          {t('common:buttons.cancel')}
        </button>
        <button
          type="submit"
          className="px-4 py-2 text-white bg-primary-500 rounded-md hover:bg-primary-600 flex items-center gap-2 disabled:opacity-50"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading
            ? t('form.saving')
            : expense
              ? t('form.updateExpense')
              : t('form.createExpense')}
          {t('form.expenseSuffix')}
        </button>
      </div>
    </form>
  );
};
