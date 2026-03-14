import { useState, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useCreateFee, useUpdateFee } from '@/hooks/usePropertyFinancialsHooks';
import {
  PropertyFeeResponse,
  CreateFeeRequest,
  UpdateFeeRequest,
  FeeType,
  FeeStatus,
  PaymentFrequency,
  formatFeeType,
  formatFeeStatus,
  formatPaymentFrequency,
} from '@/types/propertyFinancials';

interface FeeFormModalProps {
  propertyId: string;
  existing?: PropertyFeeResponse;
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

export const FeeFormModal = ({
  propertyId,
  existing,
  onClose,
}: FeeFormModalProps) => {
  const { defaultCurrency } = useTeamDefaults();
  const currency = defaultCurrency || 'EUR';
  const createMutation = useCreateFee(propertyId);
  const updateMutation = useUpdateFee(propertyId);
  const isEdit = !!existing;
  const isPending = createMutation.isPending || updateMutation.isPending;

  const [formData, setFormData] = useState<CreateFeeRequest>({
    feeType: existing?.feeType ?? FeeType.HOA,
    name: existing?.name ?? '',
    annualAmount: existing?.annualAmount ?? 0,
    currency: existing?.currency ?? currency,
    paymentFrequency: existing?.paymentFrequency ?? PaymentFrequency.MONTHLY,
    dueMonths: existing?.dueMonths,
    startDate: existing?.startDate ?? '',
    endDate: existing?.endDate ?? '',
    status: existing?.status ?? FeeStatus.ACTIVE,
    notes: existing?.notes ?? '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const payload = {
      ...formData,
      name: formData.name || undefined,
      startDate: formData.startDate || undefined,
      endDate: formData.endDate || undefined,
      notes: formData.notes || undefined,
    };
    if (isEdit) {
      const updatePayload: UpdateFeeRequest = payload;
      updateMutation.mutate(
        { feeId: existing.identifier, data: updatePayload },
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
        const form = document.getElementById('fee-form') as HTMLFormElement;
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
            {isEdit ? 'Edit Fee' : 'Add Fee'}
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
          id="fee-form"
          onSubmit={handleSubmit}
          className="p-4 space-y-4 overflow-y-auto flex-1"
        >
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Fee Type *
              </label>
              <select
                value={formData.feeType}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    feeType: e.target.value as FeeType,
                  })
                }
                required
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              >
                {Object.values(FeeType).map((type) => (
                  <option key={type} value={type}>
                    {formatFeeType(type)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Name
              </label>
              <input
                type="text"
                value={formData.name ?? ''}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Annual Amount *
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
                Payment Frequency
              </label>
              <select
                value={formData.paymentFrequency ?? PaymentFrequency.MONTHLY}
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
                Status
              </label>
              <select
                value={formData.status ?? FeeStatus.ACTIVE}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    status: e.target.value as FeeStatus,
                  })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              >
                {Object.values(FeeStatus).map((s) => (
                  <option key={s} value={s}>
                    {formatFeeStatus(s)}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Due Months
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
                Start Date
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
                End Date
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

            <div className="col-span-2">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Notes
              </label>
              <RichTextEditor
                value={formData.notes ?? ''}
                onChange={(value) =>
                  setFormData({ ...formData, notes: value || undefined })
                }
                placeholder="Add notes..."
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
            Cancel
          </button>
          <button
            type="submit"
            form="fee-form"
            disabled={isPending}
            className="bg-primary-500 text-white px-4 py-2 text-sm font-medium rounded-md hover:bg-primary-600 disabled:opacity-50"
          >
            {isPending ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
};
