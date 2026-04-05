import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useCreateTax, useUpdateTax } from '@/hooks/usePropertyFinancialsHooks';
import {
  PropertyTaxResponse,
  CreateTaxRequest,
  UpdateTaxRequest,
  TaxType,
  TaxStatus,
  PaymentFrequency,
  formatTaxType,
  formatTaxStatus,
  formatPaymentFrequency,
} from '@/types/propertyFinancials';

interface TaxFormModalProps {
  propertyId: string;
  existing?: PropertyTaxResponse;
  onClose: () => void;
}

const MONTH_OPTIONS = [
  { value: 1, label: 'Jan' },
  { value: 2, label: 'Feb' },
  { value: 3, label: 'Mar' },
  { value: 4, label: 'Apr' },
  { value: 5, label: 'May' },
  { value: 6, label: 'Jun' },
  { value: 7, label: 'Jul' },
  { value: 8, label: 'Aug' },
  { value: 9, label: 'Sep' },
  { value: 10, label: 'Oct' },
  { value: 11, label: 'Nov' },
  { value: 12, label: 'Dec' },
];

const FREQUENCY_MONTHS: Record<string, number[]> = {
  MONTHLY: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
  QUARTERLY: [1, 4, 7, 10],
  SEMI_ANNUALLY: [1, 7],
  ANNUALLY: [1],
};

function detectFrequency(months: number[]): PaymentFrequency {
  const sorted = [...months].sort((a, b) => a - b);
  const key = sorted.join(',');
  for (const [freq, preset] of Object.entries(FREQUENCY_MONTHS)) {
    if (key === preset.join(',')) {
      return freq as PaymentFrequency;
    }
  }
  return PaymentFrequency.CUSTOM;
}

function getMonthsForFrequency(freq: PaymentFrequency): string | undefined {
  const months = FREQUENCY_MONTHS[freq];
  if (!months) {
    return undefined;
  }
  if (months.length === 12) {
    return undefined;
  }
  return months.join(',');
}

function MonthMultiSelect({
  value,
  onChange,
  disabled,
}: {
  value: string | undefined;
  onChange: (val: string | undefined) => void;
  disabled?: boolean;
}) {
  const selected = new Set(
    value
      ? value
          .split(',')
          .map((s) => parseInt(s.trim(), 10))
          .filter((n) => !isNaN(n))
      : []
  );
  const allSelected = selected.size === 0;

  const toggle = (month: number) => {
    const next = new Set(selected);
    if (next.has(month)) {
      next.delete(month);
    } else {
      next.add(month);
    }
    if (next.size === 0 || next.size === 12) {
      onChange(undefined);
    } else {
      onChange(
        Array.from(next)
          .sort((a, b) => a - b)
          .join(',')
      );
    }
  };

  return (
    <div className="flex flex-wrap gap-1">
      <button
        type="button"
        onClick={() => onChange(undefined)}
        disabled={disabled}
        className={`text-xs px-2 py-1 rounded transition-colors ${allSelected ? 'bg-primary-500 text-white' : 'bg-surface-inset text-text-secondary '} disabled:opacity-50`}
      >
        All
      </button>
      {MONTH_OPTIONS.map((m) => (
        <button
          key={m.value}
          type="button"
          onClick={() => toggle(m.value)}
          disabled={disabled}
          className={`text-xs px-2 py-1 rounded transition-colors ${!allSelected && selected.has(m.value) ? 'bg-primary-500 text-white' : 'bg-surface-inset text-text-secondary '} disabled:opacity-50`}
        >
          {m.label}
        </button>
      ))}
    </div>
  );
}

export const TaxFormModal = ({
  propertyId,
  existing,
  onClose,
}: TaxFormModalProps) => {
  const { t } = useTranslation('properties');
  const { defaultCurrency } = useTeamDefaults();
  const currency = defaultCurrency || 'EUR';
  const createMutation = useCreateTax(propertyId);
  const updateMutation = useUpdateTax(propertyId);
  const isEdit = !!existing;
  const isPending = createMutation.isPending || updateMutation.isPending;

  const [formData, setFormData] = useState<CreateTaxRequest>({
    taxType: existing?.taxType ?? TaxType.PROPERTY,
    authority: existing?.authority ?? '',
    annualAmount: existing?.annualAmount ?? 0,
    currency: existing?.currency ?? currency,
    paymentFrequency: existing?.paymentFrequency ?? PaymentFrequency.ANNUALLY,
    dueMonths: existing?.dueMonths,
    taxYear: existing?.taxYear,
    startDate: existing?.startDate ?? '',
    endDate: existing?.endDate ?? '',
    status: existing?.status ?? TaxStatus.ACTIVE,
    notes: existing?.notes ?? '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const payload = {
      ...formData,
      authority: formData.authority || undefined,
      startDate: formData.startDate || undefined,
      endDate: formData.endDate || undefined,
      notes: formData.notes || undefined,
      taxYear: formData.taxYear || undefined,
    };
    if (isEdit) {
      const updatePayload: UpdateTaxRequest = payload;
      updateMutation.mutate(
        { taxId: existing.identifier, data: updatePayload },
        { onSuccess: onClose }
      );
    } else {
      createMutation.mutate(payload, { onSuccess: onClose });
    }
  };

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault();
        const form = document.getElementById('tax-form') as HTMLFormElement;
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

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4 max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between p-4 border-b border-border-strong flex-shrink-0">
          <h2 className="text-lg font-semibold text-text-primary">
            {isEdit
              ? t('financials.modals.editTax')
              : t('financials.modals.addTax')}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-text-secondary hover:text-text-secondary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form
          id="tax-form"
          onSubmit={handleSubmit}
          className="p-4 space-y-4 overflow-y-auto flex-1"
        >
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.taxType')} *
              </label>
              <select
                value={formData.taxType}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    taxType: e.target.value as TaxType,
                  })
                }
                required
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              >
                {Object.values(TaxType).map((type) => (
                  <option key={type} value={type}>
                    {formatTaxType(type)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.authority')}
              </label>
              <input
                type="text"
                value={formData.authority ?? ''}
                onChange={(e) =>
                  setFormData({ ...formData, authority: e.target.value })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.annualAmount')} *
              </label>
              <MoneyInput
                value={formData.annualAmount}
                onChange={(v) =>
                  setFormData({ ...formData, annualAmount: v ?? 0 })
                }
                currency={formData.currency ?? currency}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.paymentFrequency')}
              </label>
              <select
                value={formData.paymentFrequency ?? PaymentFrequency.ANNUALLY}
                onChange={(e) => {
                  const freq = e.target.value as PaymentFrequency;
                  if (freq === PaymentFrequency.CUSTOM) {
                    setFormData({ ...formData, paymentFrequency: freq });
                  } else {
                    setFormData({
                      ...formData,
                      paymentFrequency: freq,
                      dueMonths: getMonthsForFrequency(freq),
                    });
                  }
                }}
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              >
                {Object.values(PaymentFrequency).map((freq) => (
                  <option key={freq} value={freq}>
                    {formatPaymentFrequency(freq)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.taxYear')}
              </label>
              <input
                type="number"
                min={1900}
                max={2100}
                value={formData.taxYear ?? ''}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    taxYear: e.target.value
                      ? Number(e.target.value)
                      : undefined,
                  })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.dueMonths')}
              </label>
              <MonthMultiSelect
                value={formData.dueMonths}
                onChange={(val) => {
                  const months = val
                    ? val
                        .split(',')
                        .map((s) => parseInt(s.trim(), 10))
                        .filter((n) => !isNaN(n))
                    : [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
                  setFormData({
                    ...formData,
                    dueMonths: val,
                    paymentFrequency: detectFrequency(months),
                  });
                }}
                disabled={isPending}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.startDate')}
              </label>
              <input
                type="date"
                value={formData.startDate ?? ''}
                onChange={(e) =>
                  setFormData({ ...formData, startDate: e.target.value })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.endDate')}
              </label>
              <input
                type="date"
                value={formData.endDate ?? ''}
                onChange={(e) =>
                  setFormData({ ...formData, endDate: e.target.value })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.status')}
              </label>
              <select
                value={formData.status ?? TaxStatus.ACTIVE}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    status: e.target.value as TaxStatus,
                  })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              >
                {Object.values(TaxStatus).map((s) => (
                  <option key={s} value={s}>
                    {formatTaxStatus(s)}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('financials.labels.notes')}
              </label>
              <RichTextEditor
                value={formData.notes ?? ''}
                onChange={(value) =>
                  setFormData({ ...formData, notes: value || undefined })
                }
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
            form="tax-form"
            disabled={isPending}
            className="bg-primary-500 text-white px-4 py-2 text-sm font-medium rounded-md hover:bg-primary-600 disabled:opacity-50"
          >
            {isPending ? t('financials.saving') : t('common:buttons.save')}
          </button>
        </div>
      </div>
    </div>
  );
};
