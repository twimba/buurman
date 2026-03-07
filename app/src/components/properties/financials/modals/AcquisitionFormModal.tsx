import { useState, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4">
        <div className="flex items-center justify-between p-4 border-b border-[#c9cfd9] dark:border-[#3a3f54]">
          <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            {existing ? 'Edit Acquisition' : 'Add Acquisition'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-[#6b7194] hover:text-[#3d4463] dark:text-[#8b90a8] dark:hover:text-[#c4c8db]"
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
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Acquisition Type
            </label>
            <select
              value={formData.acquisitionType}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  acquisitionType: e.target.value as AcquisitionType,
                })
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            >
              {Object.values(AcquisitionType).map((type) => (
                <option key={type} value={type}>
                  {formatAcquisitionType(type)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Acquisition Date
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
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Purchase Price
            </label>
            <MoneyInput
              value={formData.purchasePrice}
              onChange={(v) => setFormData({ ...formData, purchasePrice: v })}
              currency={formData.purchasePriceCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Closing Costs
            </label>
            <MoneyInput
              value={formData.closingCosts}
              onChange={(v) => setFormData({ ...formData, closingCosts: v })}
              currency={formData.closingCostsCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Renovation Costs
            </label>
            <MoneyInput
              value={formData.renovationCosts}
              onChange={(v) => setFormData({ ...formData, renovationCosts: v })}
              currency={formData.renovationCostsCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Land Value
            </label>
            <MoneyInput
              value={formData.landValue}
              onChange={(v) => setFormData({ ...formData, landValue: v })}
              currency={formData.landValueCurrency ?? currency}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Depreciation Method
            </label>
            <select
              value={formData.depreciationMethod ?? DepreciationMethod.NONE}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  depreciationMethod: e.target.value as DepreciationMethod,
                })
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            >
              {Object.values(DepreciationMethod).map((method) => (
                <option key={method} value={method}>
                  {formatDepreciationMethod(method)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Depreciation Years
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
                className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 pr-14 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                years
              </span>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
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
        </form>

        <div className="flex justify-end gap-3 p-4 border-t border-[#c9cfd9] dark:border-[#3a3f54]">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
          >
            Cancel
          </button>
          <button
            type="submit"
            form="acquisition-form"
            disabled={mutation.isPending}
            className="bg-[#5c7cfa] text-white px-4 py-2 text-sm font-medium rounded-md hover:bg-[#4c6ef5] disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
};
