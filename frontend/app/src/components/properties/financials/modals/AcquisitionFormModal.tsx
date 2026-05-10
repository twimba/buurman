import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useUpsertAcquisition } from '@/hooks/usePropertyFinancialsHooks';
import {
  PropertyAcquisitionResponse,
  UpsertAcquisitionRequest,
  AcquisitionType,
  DepreciationMethod,
  formatAcquisitionType,
  formatDepreciationMethod,
} from '@/types/propertyFinancials';

interface AcquisitionFormModalProps {
  propertyId: string;
  existing?: PropertyAcquisitionResponse;
  onClose: () => void;
}

export const AcquisitionFormModal = ({
  propertyId,
  existing,
  onClose,
}: AcquisitionFormModalProps) => {
  const { t } = useTranslation('properties');
  const { defaultCurrency } = useTeamDefaults();
  const currency = defaultCurrency || 'EUR';
  const mutation = useUpsertAcquisition(propertyId);

  const [formData, setFormData] = useState<UpsertAcquisitionRequest>({
    acquisitionType: existing?.acquisitionType ?? AcquisitionType.PURCHASE,
    acquisitionDate: existing?.acquisitionDate ?? '',
    purchasePrice: existing?.purchasePrice,
    purchasePriceCurrency: existing?.purchasePriceCurrency ?? currency,
    closingCosts: existing?.closingCosts,
    closingCostsCurrency: existing?.closingCostsCurrency ?? currency,
    renovationCosts: existing?.renovationCosts,
    renovationCostsCurrency: existing?.renovationCostsCurrency ?? currency,
    landValue: existing?.landValue,
    landValueCurrency: existing?.landValueCurrency ?? currency,
    depreciationMethod: existing?.depreciationMethod ?? DepreciationMethod.NONE,
    depreciationYears: existing?.depreciationYears,
    notes: existing?.notes ?? '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData, { onSuccess: onClose });
  };

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault();
        const form = document.getElementById(
          'acquisition-form'
        ) as HTMLFormElement;
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
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4">
        <div className="flex items-center justify-between p-4 border-b border-border-strong">
          <h2 className="text-lg font-semibold text-text-primary">
            {existing
              ? t('financials.modals.editAcquisition')
              : t('financials.modals.addAcquisition')}
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
          id="acquisition-form"
          onSubmit={handleSubmit}
          className="p-4 space-y-4 max-h-[70vh] overflow-y-auto"
        >
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.acquisitionType')}
            </label>
            <select
              value={formData.acquisitionType}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  acquisitionType: e.target.value as AcquisitionType,
                })
              }
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            >
              {Object.values(AcquisitionType).map((type) => (
                <option key={type} value={type}>
                  {formatAcquisitionType(type)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.acquisitionDate')}
            </label>
            <input
              type="date"
              value={formData.acquisitionDate ?? ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  acquisitionDate: e.target.value || undefined,
                })
              }
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.purchasePrice')}
            </label>
            <MoneyInput
              value={formData.purchasePrice}
              onChange={(v) => setFormData({ ...formData, purchasePrice: v })}
              currency={formData.purchasePriceCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.closingCosts')}
            </label>
            <MoneyInput
              value={formData.closingCosts}
              onChange={(v) => setFormData({ ...formData, closingCosts: v })}
              currency={formData.closingCostsCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.renovationCosts')}
            </label>
            <MoneyInput
              value={formData.renovationCosts}
              onChange={(v) => setFormData({ ...formData, renovationCosts: v })}
              currency={formData.renovationCostsCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.landValue')}
            </label>
            <MoneyInput
              value={formData.landValue}
              onChange={(v) => setFormData({ ...formData, landValue: v })}
              currency={formData.landValueCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.depreciationMethod')}
            </label>
            <select
              value={formData.depreciationMethod ?? DepreciationMethod.NONE}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  depreciationMethod: e.target.value as DepreciationMethod,
                })
              }
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            >
              {Object.values(DepreciationMethod).map((method) => (
                <option key={method} value={method}>
                  {formatDepreciationMethod(method)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('financials.labels.depreciationYears')}
            </label>
            <div className="relative">
              <input
                type="number"
                min={0}
                value={formData.depreciationYears ?? ''}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    depreciationYears: e.target.value
                      ? Number(e.target.value)
                      : undefined,
                  })
                }
                className="w-full border border-border-strong rounded-md px-3 py-2 pr-14 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-text-secondary">
                {t('financials.units.years')}
              </span>
            </div>
          </div>

          <div>
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
        </form>

        <div className="flex justify-end gap-3 p-4 border-t border-border-strong">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
          >
            {t('common:buttons.cancel')}
          </button>
          <button
            type="submit"
            form="acquisition-form"
            disabled={mutation.isPending}
            className="bg-primary-500 text-white px-4 py-2 text-sm font-medium rounded-md hover:bg-primary-600 disabled:opacity-50"
          >
            {mutation.isPending
              ? t('financials.saving')
              : t('common:buttons.save')}
          </button>
        </div>
      </div>
    </div>
  );
};
