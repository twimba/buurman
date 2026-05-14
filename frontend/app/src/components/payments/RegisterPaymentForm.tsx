import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, CalendarCheck } from 'lucide-react';
import { CreatePaymentRequest } from '@/types/payment';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
import { ContactSelector } from '@/components/common/ContactSelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

interface RegisterPaymentFormProps {
  onSubmit: (data: CreatePaymentRequest) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  contractIdentifier: string;
  resetKey?: number;
  continueAdding?: boolean;
  onContinueAddingChange?: (value: boolean) => void;
}

export const RegisterPaymentForm = ({
  onSubmit,
  onCancel,
  isLoading,
  contractIdentifier,
  resetKey,
  continueAdding,
  onContinueAddingChange,
}: RegisterPaymentFormProps) => {
  const { t } = useTranslation('payments');
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState({
    amount: 0,
    currency: defaultCurrency || '',
    paymentDate: new Date().toISOString().split('T')[0],
    notes: '',
    contactIdentifier: undefined as string | undefined,
  });

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
    if (resetKey === undefined || resetKey === 0) {
      return;
    }
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) => ({
      ...prev,
      amount: 0,
      paymentDate: new Date().toISOString().split('T')[0],
      notes: '',
      contactIdentifier: undefined,
    }));
    setErrors({});
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [resetKey]);

  const currency = formData.currency || defaultCurrency || '';

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (formData.amount <= 0) {
      newErrors.amount = t('validation.amountRequired');
    }
    if (formData.amount > 0 && !currency.trim()) {
      newErrors.currency = t('validation.currencyRequired');
    }
    if (!formData.paymentDate) {
      newErrors.paymentDate = t('validation.paymentDateRequired');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }
    await onSubmit({
      contractIdentifier,
      contactIdentifier: formData.contactIdentifier,
      amount: formData.amount,
      currency: formData.currency,
      dueDate: formData.paymentDate,
      paymentDate: formData.paymentDate,
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
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.amount')} <span className="text-error-text">*</span>
        </label>
        <MoneyInput
          value={formData.amount ?? undefined}
          onChange={(val) => setFormData({ ...formData, amount: val ?? 0 })}
          currency={currency}
          disabled={isLoading}
          error={!!errors.amount || !!errors.currency}
        />
        {(errors.amount || errors.currency) && (
          <p className="mt-1 text-sm text-error-text">
            {errors.amount || errors.currency}
          </p>
        )}
      </div>

      {/* Payment Date */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.paymentDate')} <span className="text-error-text">*</span>
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
                ? 'border-error-border'
                : 'border-border-strong'
            } bg-surface-card text-text-primary `}
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
            className="px-3 py-2 text-sm bg-surface-inset hover:bg-surface-raised border border-border-strong rounded-md transition-colors text-text-secondary"
            disabled={isLoading}
          >
            {t('form.today')}
          </button>
        </div>
        {errors.paymentDate && (
          <p className="mt-1 text-sm text-error-text">{errors.paymentDate}</p>
        )}
      </div>

      {/* Contact (optional) */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.contact')}
        </label>
        <div className="flex gap-2">
          <div className="flex-1">
            <ContactSelector
              value={formData.contactIdentifier}
              onChange={(value) =>
                setFormData({ ...formData, contactIdentifier: value })
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
              className="px-3 py-2 text-sm bg-surface-inset hover:bg-surface-raised border border-border-strong rounded-md transition-colors"
              disabled={isLoading}
            >
              {t('form.clear')}
            </button>
          )}
        </div>
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-2">
          {t('form.notes')}
        </label>
        <RichTextEditor
          value={formData.notes}
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
          className="px-4 py-2 text-white bg-primary-500 rounded-md hover:bg-primary-600 flex items-center gap-1.5 disabled:opacity-50"
          disabled={isLoading}
        >
          <CalendarCheck className="h-4 w-4" />
          {isLoading ? t('form.saving') : t('actions.registerPayment')}
        </button>
      </div>
    </form>
  );
};
