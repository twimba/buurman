import { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import {
  useCreateInsurance,
  useUpdateInsurance,
} from '@/hooks/usePropertyFinancialsHooks';
import {
  PropertyInsuranceResponse,
  CreateInsuranceRequest,
  UpdateInsuranceRequest,
  InsuranceType,
  InsuranceStatus,
  PaymentFrequency,
  formatInsuranceType,
  formatInsuranceStatus,
  formatPaymentFrequency,
} from '@/types/propertyFinancials';

interface InsuranceFormModalProps {
  propertyId: string;
  existing?: PropertyInsuranceResponse;
  onClose: () => void;
}

export const InsuranceFormModal = ({
  propertyId,
  existing,
  onClose,
}: InsuranceFormModalProps) => {
  const { defaultCurrency } = useTeamDefaults();
  const createMutation = useCreateInsurance(propertyId);
  const updateMutation = useUpdateInsurance(propertyId);
  const isLoading = createMutation.isPending || updateMutation.isPending;

  const [formData, setFormData] = useState<
    CreateInsuranceRequest | UpdateInsuranceRequest
  >({
    insuranceType: existing?.insuranceType ?? InsuranceType.BUILDING,
    provider: existing?.provider ?? '',
    policyNumber: existing?.policyNumber ?? '',
    coverageAmount: existing?.coverageAmount ?? undefined,
    coverageAmountCurrency:
      existing?.coverageAmountCurrency ?? defaultCurrency ?? 'EUR',
    annualPremium: existing?.annualPremium ?? (undefined as unknown as number),
    annualPremiumCurrency:
      existing?.annualPremiumCurrency ?? defaultCurrency ?? 'EUR',
    paymentFrequency: existing?.paymentFrequency ?? PaymentFrequency.ANNUALLY,
    startDate: existing?.startDate ?? '',
    endDate: existing?.endDate ?? '',
    status: existing?.status ?? InsuranceStatus.ACTIVE,
    notes: existing?.notes ?? '',
  });

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (existing) {
      updateMutation.mutate(
        { insuranceId: existing.identifier, data: formData },
        { onSuccess: onClose }
      );
    } else {
      createMutation.mutate(formData as CreateInsuranceRequest, {
        onSuccess: onClose,
      });
    }
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault();
        handleSubmit();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  });

  const update = (field: string, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const inputClass =
    'w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]';
  const labelClass =
    'block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1';

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4 max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between p-4 border-b border-[#c9cfd9] dark:border-[#3a3f54] flex-shrink-0">
          <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            {existing ? 'Edit Insurance' : 'Add Insurance'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]"
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
                Insurance Type <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.insuranceType ?? ''}
                onChange={(e) => update('insuranceType', e.target.value)}
                className={inputClass}
                required
              >
                {Object.values(InsuranceType).map((t) => (
                  <option key={t} value={t}>
                    {formatInsuranceType(t)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className={labelClass}>Provider</label>
              <input
                type="text"
                value={formData.provider ?? ''}
                onChange={(e) =>
                  update('provider', e.target.value || undefined)
                }
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>Policy Number</label>
              <input
                type="text"
                value={formData.policyNumber ?? ''}
                onChange={(e) =>
                  update('policyNumber', e.target.value || undefined)
                }
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>Coverage Amount</label>
              <MoneyInput
                value={formData.coverageAmount}
                onChange={(v) => update('coverageAmount', v)}
                currency={
                  formData.coverageAmountCurrency ?? defaultCurrency ?? 'EUR'
                }
              />
            </div>

            <div className="min-w-0">
              <label className={labelClass}>
                Annual Premium <span className="text-red-500">*</span>
              </label>
              <MoneyInput
                value={formData.annualPremium}
                onChange={(v) => update('annualPremium', v)}
                currency={
                  formData.annualPremiumCurrency ?? defaultCurrency ?? 'EUR'
                }
              />
            </div>

            <div>
              <label className={labelClass}>Payment Frequency</label>
              <select
                value={formData.paymentFrequency ?? ''}
                onChange={(e) =>
                  update('paymentFrequency', e.target.value || undefined)
                }
                className={inputClass}
              >
                {Object.values(PaymentFrequency).map((f) => (
                  <option key={f} value={f}>
                    {formatPaymentFrequency(f)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className={labelClass}>Start Date</label>
              <input
                type="date"
                value={formData.startDate ?? ''}
                onChange={(e) =>
                  update('startDate', e.target.value || undefined)
                }
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>End Date</label>
              <input
                type="date"
                value={formData.endDate ?? ''}
                onChange={(e) => update('endDate', e.target.value || undefined)}
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>Status</label>
              <select
                value={formData.status ?? ''}
                onChange={(e) => update('status', e.target.value || undefined)}
                className={inputClass}
              >
                {Object.values(InsuranceStatus).map((s) => (
                  <option key={s} value={s}>
                    {formatInsuranceStatus(s)}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-span-2">
              <label className={labelClass}>Notes</label>
              <RichTextEditor
                value={formData.notes ?? ''}
                onChange={(value) => update('notes', value || undefined)}
                placeholder="Add notes..."
              />
            </div>
          </div>
        </form>

        <div className="flex justify-end gap-3 p-4 border-t border-[#c9cfd9] dark:border-[#3a3f54] flex-shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
          >
            Cancel
          </button>
          <button
            type="submit"
            onClick={handleSubmit}
            disabled={isLoading}
            className="bg-[#5c7cfa] text-white px-4 py-2 text-sm font-medium rounded-md hover:bg-[#4c6ef5] disabled:opacity-50"
          >
            {isLoading ? 'Saving...' : existing ? 'Save Changes' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
};
