import { useState, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import { MoneyInput } from '@/components/common/MoneyInput';
import { RichTextEditor } from '@/components/common/RichTextEditor';
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
    totalAmount: existing?.totalAmount ?? (undefined as unknown as number),
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
            {existing ? 'Edit Payment' : 'Record Payment'}
          </h2>
          <button
            onClick={onClose}
            className="text-[#8a8fa8] hover:text-[#3d4463] dark:hover:text-[#eef0f6]"
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
              <label className={labelClass}>Payment Date *</label>
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
              <label className={labelClass}>Status</label>
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
              <label className={labelClass}>Total Amount *</label>
              <MoneyInput
                value={formData.totalAmount as number}
                onChange={(v) => update('totalAmount', v)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}

              />
            </div>

            {/* Principal Amount */}
            <div>
              <label className={labelClass}>Principal</label>
              <MoneyInput
                value={formData.principalAmount as number}
                onChange={(v) => update('principalAmount', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}

              />
            </div>

            {/* Interest Amount */}
            <div>
              <label className={labelClass}>Interest</label>
              <MoneyInput
                value={formData.interestAmount as number}
                onChange={(v) => update('interestAmount', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}

              />
            </div>

            {/* Escrow Amount */}
            <div>
              <label className={labelClass}>Escrow</label>
              <MoneyInput
                value={formData.escrowAmount as number}
                onChange={(v) => update('escrowAmount', v ?? undefined)}
                currency={formData.currency ?? defaultCurrency ?? 'EUR'}

              />
            </div>

            {/* Extra Payment */}
            <div>
              <label className={labelClass}>Extra Payment</label>
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
              className="h-4 w-4 rounded border-[#c9cfd9] dark:border-[#3a3f54] text-[#5c7cfa] focus:ring-[#5c7cfa]"
            />
            <label
              htmlFor="deductFromBalance"
              className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
            >
              Deduct principal from financing balance
            </label>
          </div>

          {/* Notes */}
          <div>
            <label className={labelClass}>Notes</label>
            <RichTextEditor
              value={(formData.notes as string) ?? ''}
              onChange={(value) => update('notes', value || undefined)}
              placeholder="Add notes..."
            />
          </div>

          {/* Documents (only when editing existing payment) */}
          {existing && (
            <div>
              <label className={labelClass}>Documents</label>
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
            {isLoading
              ? 'Saving...'
              : existing
                ? 'Save Changes'
                : 'Record Payment'}
          </button>
        </div>
      </div>
    </div>
  );
};
