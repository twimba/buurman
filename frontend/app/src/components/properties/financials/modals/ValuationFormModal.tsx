import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import {
  useCreateValuation,
  useUpdateValuation,
} from '@/hooks/usePropertyFinancialsHooks';
import {
  PropertyValuationResponse,
  CreateValuationRequest,
  UpdateValuationRequest,
  ValuationType,
  formatValuationType,
} from '@/types/propertyFinancials';

interface ValuationFormModalProps {
  propertyId: string;
  existing?: PropertyValuationResponse;
  onClose: () => void;
}

export const ValuationFormModal = ({
  propertyId,
  existing,
  onClose,
}: ValuationFormModalProps) => {
  const { t } = useTranslation('properties');
  const { defaultCurrency } = useTeamDefaults();
  const currency = defaultCurrency || 'EUR';
  const createMutation = useCreateValuation(propertyId);
  const updateMutation = useUpdateValuation(propertyId);
  const isPending = createMutation.isPending || updateMutation.isPending;

  const [formData, setFormData] = useState<CreateValuationRequest>({
    valuationType: existing?.valuationType ?? ValuationType.MARKET,
    valuationDate: existing?.valuationDate ?? '',
    amount: existing?.amount ?? 0,
    currency: existing?.currency ?? currency,
    source: existing?.source ?? '',
    notes: existing?.notes ?? '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.valuationDate || !formData.amount) {
      return;
    }
    if (existing) {
      const data: UpdateValuationRequest = {
        valuationType: formData.valuationType,
        valuationDate: formData.valuationDate,
        amount: formData.amount,
        currency: formData.currency,
        source: formData.source || undefined,
        notes: formData.notes || undefined,
      };
      updateMutation.mutate(
        { valuationId: existing.identifier, data },
        { onSuccess: onClose }
      );
    } else {
      const data: CreateValuationRequest = {
        ...formData,
        source: formData.source || undefined,
        notes: formData.notes || undefined,
      };
      createMutation.mutate(data, { onSuccess: onClose });
    }
  };

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault();
        const form = document.getElementById(
          'valuation-form'
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
            {existing ? t('financials.modals.editValuation') : t('financials.modals.addValuation')}
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
          id="valuation-form"
          onSubmit={handleSubmit}
          className="p-4 space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">{t('financials.labels.valuationType')}</label>
            <select
              value={formData.valuationType}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  valuationType: e.target.value as ValuationType,
                })
              }
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            >
              {Object.values(ValuationType).map((type) => (
                <option key={type} value={type}>
                  {formatValuationType(type)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">{t('financials.labels.valuationDate')} <span className="text-error-text">*</span>
            </label>
            <input
              type="date"
              required
              value={formData.valuationDate}
              onChange={(e) =>
                setFormData({ ...formData, valuationDate: e.target.value })
              }
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">{t('financials.labels.amount')} <span className="text-error-text">*</span>
            </label>
            <MoneyInput
              value={formData.amount ?? undefined}
              onChange={(v) => setFormData({ ...formData, amount: v ?? 0 })}
              currency={formData.currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">{t('financials.labels.source')}</label>
            <input
              type="text"
              value={formData.source ?? ''}
              onChange={(e) =>
                setFormData({ ...formData, source: e.target.value })
              }
              placeholder={t('financials.placeholders.source')}
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">{t('financials.labels.notes')}</label>
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
          >{t('common:buttons.cancel')}</button>
          <button
            type="submit"
            form="valuation-form"
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
