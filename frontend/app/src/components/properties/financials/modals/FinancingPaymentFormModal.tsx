import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@buurman/ui';
import { DocumentList } from '@/components/properties/DocumentList';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import {
  useCreateFinancingPayment,
  useUpdateFinancingPayment,
  useFinancingPaymentDocuments,
  useUploadFinancingPaymentDocument,
  useDeleteFinancingPaymentDocument,
} from '@/hooks/usePropertyFinancialsHooks';
import {
  FinancingPaymentResponse,
  CreateFinancingPaymentRequest,
  UpdateFinancingPaymentRequest,
  PaymentStatus,
  formatPaymentStatus,
} from '@/types/propertyFinancials';

interface FinancingPaymentFormModalProps {
  propertyId: string;
  financingId: string;
  existing?: FinancingPaymentResponse;
  onClose: () => void;
}

export const FinancingPaymentFormModal = ({
  propertyId,
  financingId,
  existing,
  onClose,
}: FinancingPaymentFormModalProps) => {
  const { t } = useTranslation('properties');
  const { defaultCurrency } = useTeamDefaults();
  const createMutation = useCreateFinancingPayment(propertyId, financingId);
  const updateMutation = useUpdateFinancingPayment(propertyId, financingId);
  const isLoading = createMutation.isPending || updateMutation.isPending;

  // Documents (only for existing payments)
  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = useFinancingPaymentDocuments(
    propertyId,
    financingId,
    existing?.identifier
  );
  const uploadDoc = useUploadFinancingPaymentDocument(
    propertyId,
    financingId,
    existing?.identifier ?? ''
  );
  const deleteDoc = useDeleteFinancingPaymentDocument(
    propertyId,
    financingId,
    existing?.identifier ?? ''
  );

  const [formData, setFormData] = useState<
    CreateFinancingPaymentRequest | UpdateFinancingPaymentRequest
  >({
    paymentDate:
      existing?.paymentDate ?? new Date().toISOString().split('T')[0],
    totalAmount: existing?.totalAmount as number | undefined,
    principalAmount: existing?.principalAmount ?? undefined,
    interestAmount: existing?.interestAmount ?? undefined,
    escrowAmount: existing?.escrowAmount ?? undefined,
    extraPayment: existing?.extraPayment ?? undefined,
    currency: existing?.currency ?? defaultCurrency ?? 'EUR',
    status: existing?.status ?? PaymentStatus.COMPLETED,
    notes: existing?.notes ?? '',
    deductFromBalance: existing ? existing.balanceDeducted : true,
  });

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (existing) {
      updateMutation.mutate(
        { paymentId: existing.identifier, data: formData },
        { onSuccess: onClose }
      );
    } else {
      createMutation.mutate(formData as CreateFinancingPaymentRequest, {
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
            {existing ? t('financials.modals.editPayment') : t('financials.modals.recordPayment')}
          </h2>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text-secondary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form
          onSubmit={handleSubmit}
          className="overflow-y-auto p-4 space-y-4 flex-1"
        >
          <div className="grid grid-cols-2 gap-4">
            {/* Payment Date */}
            <div>
              <label className={labelClass}>{t('financials.labels.paymentDate')} *</label>
              <input
                type="date"
                value={(formData.paymentDate as string) ?? ''}
                onChange={(e) => update('paymentDate', e.target.value)}
                required
                className={inputClass}
              />
            </div>

            {/* Status */}
            <div>
              <label className={labelClass}>{t('financials.labels.status')}</label>
              <select
                value={formData.status ?? PaymentStatus.COMPLETED}
                onChange={(e) =>
                  update('status', e.target.value as PaymentStatus)
                }
                className={inputClass}
              >
                {Object.values(PaymentStatus).map((s) => (
                  <option key={s} value={s}>
                    {formatPaymentStatus(s)}
                  </option>
                ))}
              </select>
            </div>

            {/* Total Amount */}
            <div className="col-span-2">
              <label className={labelClass}>{t('financials.labels.totalAmount')} *</label>
              <MoneyInput
                value={formData.totalAmount as number}
                onChange={(v) => update('totalAmount', v)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}
              />
            </div>

            {/* Principal Amount */}
            <div>
              <label className={labelClass}>{t('financials.labels.principal')}</label>
              <MoneyInput
                value={formData.principalAmount as number}
                onChange={(v) => update('principalAmount', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}
              />
            </div>

            {/* Interest Amount */}
            <div>
              <label className={labelClass}>{t('financials.labels.interest')}</label>
              <MoneyInput
                value={formData.interestAmount as number}
                onChange={(v) => update('interestAmount', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}
              />
            </div>

            {/* Escrow Amount */}
            <div>
              <label className={labelClass}>{t('financials.labels.escrow')}</label>
              <MoneyInput
                value={formData.escrowAmount as number}
                onChange={(v) => update('escrowAmount', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}
              />
            </div>

            {/* Extra Payment */}
            <div>
              <label className={labelClass}>{t('financials.labels.extraPayment')}</label>
              <MoneyInput
                value={formData.extraPayment as number}
                onChange={(v) => update('extraPayment', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}
              />
            </div>
          </div>

          {/* Deduct from Balance */}
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="deductFromBalance"
              checked={formData.deductFromBalance ?? false}
              onChange={(e) => update('deductFromBalance', e.target.checked)}
              className="h-4 w-4 rounded border-border-strong text-primary-500 focus:ring-primary-500"
            />
            <label
              htmlFor="deductFromBalance"
              className="text-sm text-text-secondary"
            >{t('financials.labels.deductFromBalance')}</label>
          </div>

          {/* Notes */}
          <div>
            <label className={labelClass}>{t('financials.labels.notes')}</label>
            <RichTextEditor
              value={(formData.notes as string) ?? ''}
              onChange={(value) => update('notes', value || undefined)}
              placeholder={t('financials.placeholders.addNotes')}
            />
          </div>

          {/* Documents (only when editing existing payment) */}
          {existing && (
            <div>
              <label className={labelClass}>{t('financials.labels.documents')}</label>
              <DocumentList
                documents={documents}
                isLoading={docsLoading}
                error={docsError}
                onUpload={async (file, title, notes) => {
                  await uploadDoc.mutateAsync({ file, title, notes });
                }}
                onDelete={async (documentId) => {
                  await deleteDoc.mutateAsync(documentId);
                }}
                isUploading={uploadDoc.isPending}
                isDeleting={deleteDoc.isPending}
              />
            </div>
          )}
        </form>

        <div className="flex justify-end gap-3 p-4 border-t border-border-strong flex-shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
          >{t('common:buttons.cancel')}</button>
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
                : t('financials.modals.recordPayment')}
          </button>
        </div>
      </div>
    </div>
  );
};
