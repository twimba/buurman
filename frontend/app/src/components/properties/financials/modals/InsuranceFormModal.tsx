import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
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
  const { t } = useTranslation('properties');
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
    annualPremium: existing?.annualPremium as number | undefined,
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
              ? t('financials.modals.editInsurance')
              : t('financials.modals.addInsurance')}
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
                {t('financials.labels.insuranceType')}{' '}
                <span className="text-error-text">*</span>
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
              <label className={labelClass}>
                {t('financials.labels.provider')}
              </label>
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
              <label className={labelClass}>
                {t('financials.labels.policyNumber')}
              </label>
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
              <label className={labelClass}>
                {t('financials.labels.coverageAmount')}
              </label>
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
                {t('financials.labels.annualPremium')}{' '}
                <span className="text-error-text">*</span>
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
              <label className={labelClass}>
                {t('financials.labels.paymentFrequency')}
              </label>
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
              <label className={labelClass}>
                {t('financials.labels.startDate')}
              </label>
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
                {t('financials.labels.status')}
              </label>
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
