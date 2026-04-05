import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { addMonths, differenceInMonths, parseISO, format } from 'date-fns';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import {
  useCreateFinancing,
  useUpdateFinancing,
} from '@/hooks/usePropertyFinancialsHooks';
import {
  PropertyFinancingResponse,
  CreateFinancingRequest,
  UpdateFinancingRequest,
  FinancingType,
  RateType,
  FinancingStatus,
  formatFinancingType,
  formatRateType,
  formatFinancingStatus,
} from '@/types/propertyFinancials';

interface FinancingFormModalProps {
  propertyId: string;
  existing?: PropertyFinancingResponse;
  onClose: () => void;
}

export const FinancingFormModal = ({
  propertyId,
  existing,
  onClose,
}: FinancingFormModalProps) => {
  const { t } = useTranslation('properties');
  const { defaultCurrency } = useTeamDefaults();
  const createMutation = useCreateFinancing(propertyId);
  const updateMutation = useUpdateFinancing(propertyId);
  const isLoading = createMutation.isPending || updateMutation.isPending;

  const [formData, setFormData] = useState<
    CreateFinancingRequest | UpdateFinancingRequest
  >({
    financingType: existing?.financingType ?? FinancingType.MORTGAGE,
    rateType: existing?.rateType ?? RateType.FIXED,
    lenderName: existing?.lenderName ?? '',
    loanNumber: existing?.loanNumber ?? '',
    originalAmount: existing?.originalAmount as number | undefined,
    originalAmountCurrency:
      existing?.originalAmountCurrency ?? defaultCurrency ?? 'EUR',
    currentBalance: existing?.currentBalance ?? undefined,
    currentBalanceCurrency:
      existing?.currentBalanceCurrency ??
      existing?.originalAmountCurrency ??
      defaultCurrency ??
      'EUR',
    interestRate: existing?.interestRate ?? undefined,
    monthlyPayment: existing?.monthlyPayment ?? undefined,
    monthlyPaymentCurrency:
      existing?.monthlyPaymentCurrency ??
      existing?.originalAmountCurrency ??
      defaultCurrency ??
      'EUR',
    paymentVariable: existing?.paymentVariable ?? false,
    startDate: existing?.startDate ?? '',
    endDate: existing?.endDate ?? '',
    termMonths: existing?.termMonths ?? undefined,
    status: existing?.status ?? FinancingStatus.ACTIVE,
    notes: existing?.notes ?? '',
  });

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (existing) {
      updateMutation.mutate(
        { financingId: existing.identifier, data: formData },
        { onSuccess: onClose }
      );
    } else {
      createMutation.mutate(formData as CreateFinancingRequest, {
        onSuccess: onClose,
      });
    }
  };

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault();
        const form = document.querySelector('form') as HTMLFormElement;
        if (form) {
          form.requestSubmit();
        }
      }
    },
    [onClose]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const update = (field: string, value: unknown) => {
    setFormData((prev) => {
      const next = { ...prev, [field]: value };

      // Auto-sync termMonths ↔ endDate
      if (field === 'termMonths' && value && next.startDate) {
        const months = value as number;
        const end = addMonths(parseISO(next.startDate as string), months);
        next.endDate = format(end, 'yyyy-MM-dd');
      }
      if (field === 'startDate' && value && next.termMonths) {
        const end = addMonths(
          parseISO(value as string),
          next.termMonths as number
        );
        next.endDate = format(end, 'yyyy-MM-dd');
      }
      if (field === 'endDate' && value && next.startDate) {
        const months = differenceInMonths(
          parseISO(value as string),
          parseISO(next.startDate as string)
        );
        if (months > 0) {
          next.termMonths = months;
        }
      }

      // Adjust currentBalance by the same delta when originalAmount changes
      if (
        field === 'originalAmount' &&
        existing &&
        next.currentBalance != null
      ) {
        const oldOriginal = prev.originalAmount as number;
        const newOriginal = value as number;
        if (oldOriginal && newOriginal) {
          const delta = newOriginal - oldOriginal;
          const adjusted = (next.currentBalance as number) + delta;
          next.currentBalance = Math.max(0, adjusted);
        }
      }

      // Variable payment clears monthly payment
      if (field === 'paymentVariable' && value === true) {
        next.monthlyPayment = undefined;
      }

      return next;
    });
  };

  const inputClass =
    'w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500';
  const labelClass = 'block text-sm font-medium text-text-secondary mb-1';

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4 max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between p-4 border-b border-border-strong flex-shrink-0">
          <h2 className="text-lg font-semibold text-text-primary">
            {existing
              ? t('financials.modals.editFinancing')
              : t('financials.modals.addFinancing')}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-text-secondary hover:text-text-primary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form
          onSubmit={handleSubmit}
          className="p-4 space-y-4 overflow-y-auto flex-1"
        >
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>
                {t('financials.labels.financingType')}{' '}
                <span className="text-error-text">*</span>
              </label>
              <select
                value={formData.financingType ?? ''}
                onChange={(e) => update('financingType', e.target.value)}
                className={inputClass}
                required
              >
                {Object.values(FinancingType).map((t) => (
                  <option key={t} value={t}>
                    {formatFinancingType(t)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.rateType')}
              </label>
              <select
                value={formData.rateType ?? ''}
                onChange={(e) =>
                  update('rateType', e.target.value || undefined)
                }
                className={inputClass}
              >
                <option value="">{t('common:selectors.select')}...</option>
                {Object.values(RateType).map((t) => (
                  <option key={t} value={t}>
                    {formatRateType(t)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.lenderName')}
              </label>
              <input
                type="text"
                value={formData.lenderName ?? ''}
                onChange={(e) =>
                  update('lenderName', e.target.value || undefined)
                }
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.loanNumber')}
              </label>
              <input
                type="text"
                value={formData.loanNumber ?? ''}
                onChange={(e) =>
                  update('loanNumber', e.target.value || undefined)
                }
                className={inputClass}
              />
            </div>

            <div className="min-w-0">
              <label className={labelClass}>
                {t('financials.labels.originalAmount')}{' '}
                <span className="text-error-text">*</span>
              </label>
              <MoneyInput
                value={formData.originalAmount}
                onChange={(v) => update('originalAmount', v)}
                currency={
                  formData.originalAmountCurrency ?? defaultCurrency ?? 'EUR'
                }
              />
            </div>

            <div className="min-w-0">
              <label className={labelClass}>
                {t('financials.labels.currentBalance')}
              </label>
              <MoneyInput
                value={formData.currentBalance}
                onChange={(v) => update('currentBalance', v)}
                currency={
                  formData.currentBalanceCurrency ?? defaultCurrency ?? 'EUR'
                }
              />
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.interestRate')} (%)
              </label>
              <div className="relative">
                <input
                  type="number"
                  value={formData.interestRate ?? ''}
                  onChange={(e) =>
                    update(
                      'interestRate',
                      e.target.value ? parseFloat(e.target.value) : undefined
                    )
                  }
                  step="0.001"
                  min="0"
                  max="100"
                  className={inputClass + ' pr-8'}
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary text-sm">
                  %
                </span>
              </div>
            </div>

            <div
              className={`min-w-0 ${formData.paymentVariable ? 'opacity-50 pointer-events-none' : ''}`}
            >
              <label className={labelClass}>
                {t('financials.labels.monthlyPayment')}
              </label>
              <MoneyInput
                value={formData.monthlyPayment}
                onChange={(v) => update('monthlyPayment', v)}
                currency={
                  formData.monthlyPaymentCurrency ?? defaultCurrency ?? 'EUR'
                }
              />
            </div>

            <div className="flex items-end">
              <label className="inline-flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={formData.paymentVariable ?? false}
                  onChange={(e) => update('paymentVariable', e.target.checked)}
                  className="rounded border-border-strong text-primary-500 focus:ring-primary-500"
                />
                <span className="text-sm text-text-secondary">
                  {t('financials.labels.variablePayment')}
                </span>
              </label>
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.startDate')}{' '}
                <span className="text-error-text">*</span>
              </label>
              <input
                type="date"
                value={formData.startDate ?? ''}
                onChange={(e) => update('startDate', e.target.value)}
                className={inputClass}
                required
              />
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.endDate')}
              </label>
              <input
                type="date"
                value={formData.endDate ?? ''}
                onChange={(e) => update('endDate', e.target.value || undefined)}
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.termMonths')}
              </label>
              <input
                type="number"
                value={formData.termMonths ?? ''}
                onChange={(e) =>
                  update(
                    'termMonths',
                    e.target.value ? parseInt(e.target.value, 10) : undefined
                  )
                }
                min="1"
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>
                {t('financials.labels.status')}
              </label>
              <select
                value={formData.status ?? ''}
                onChange={(e) => update('status', e.target.value || undefined)}
                className={inputClass}
              >
                {Object.values(FinancingStatus).map((s) => (
                  <option key={s} value={s}>
                    {formatFinancingStatus(s)}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-span-2">
              <label className={labelClass}>
                {t('financials.labels.notes')}
              </label>
              <RichTextEditor
                value={formData.notes ?? ''}
                onChange={(value) => update('notes', value || undefined)}
                placeholder={t('financials.placeholders.addNotes')}
              />
            </div>
          </div>
        </form>

        <div className="flex justify-end gap-3 p-4 border-t border-border-strong flex-shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
          >
            {t('common:buttons.cancel')}
          </button>
          <button
            type="submit"
            onClick={handleSubmit}
            disabled={isLoading}
            className="bg-primary-500 text-white px-4 py-2 text-sm font-medium rounded-md hover:bg-primary-600 disabled:opacity-50"
          >
            {isLoading
              ? t('financials.saving')
              : existing
                ? t('common:buttons.saveChanges')
                : t('common:buttons.create')}
          </button>
        </div>
      </div>
    </div>
  );
};
