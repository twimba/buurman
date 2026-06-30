import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import {
  BulkDataGrid,
  BulkColumnDef,
  RowData,
} from '@/components/common/BulkDataGrid';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { bulkCreateFinancingPayments } from '@/generated/api/property-financings/property-financings';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '@/utils/errorMessages';
import {
  PaymentStatus,
  formatPaymentStatus,
  CreateFinancingPaymentRequest,
} from '@/types/propertyFinancials';

interface BulkFinancingPaymentModalProps {
  propertyId: string;
  financingId: string;
  financingCurrency: string;
  onClose: () => void;
}

const BULK_COLUMNS: BulkColumnDef[] = [
  { key: 'date', label: 'Date', type: 'date', required: true },
  { key: 'totalAmount', label: 'Total Amount', type: 'number', required: true },
  { key: 'principal', label: 'Principal', type: 'number' },
  { key: 'interest', label: 'Interest', type: 'number' },
  { key: 'escrow', label: 'Escrow', type: 'number' },
  { key: 'extraPayment', label: 'Extra Payment', type: 'number' },
  {
    key: 'status',
    label: 'Status',
    type: 'select',
    options: Object.values(PaymentStatus).map((s) => ({
      value: s,
      label: formatPaymentStatus(s),
    })),
    defaultValue: PaymentStatus.COMPLETED,
  },
];

export const BulkFinancingPaymentModal = ({
  propertyId,
  financingId,
  financingCurrency,
  onClose,
}: BulkFinancingPaymentModalProps) => {
  const { t } = useTranslation('properties');
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { defaultCurrency, defaultDateFormat } = useTeamDefaults();

  const [currency, setCurrency] = useState(
    financingCurrency || defaultCurrency || 'EUR'
  );
  const [deductFromBalance, setDeductFromBalance] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    },
    [onClose]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const handleSubmit = (
    rows: RowData[],
    callbacks: {
      onRowStart: (index: number) => void;
      onRowSuccess: (index: number) => void;
      onRowError: (index: number, error: string) => void;
      onComplete: () => void;
    }
  ) => {
    setIsSubmitting(true);

    const items: CreateFinancingPaymentRequest[] = rows.map((row) => ({
      paymentDate: row.date,
      totalAmount: parseFloat(row.totalAmount),
      principalAmount: row.principal ? parseFloat(row.principal) : undefined,
      interestAmount: row.interest ? parseFloat(row.interest) : undefined,
      escrowAmount: row.escrow ? parseFloat(row.escrow) : undefined,
      extraPayment: row.extraPayment ? parseFloat(row.extraPayment) : undefined,
      currency,
      status: (row.status as PaymentStatus) ?? PaymentStatus.COMPLETED,
      deductFromBalance,
    }));

    rows.forEach((_, i) => callbacks.onRowStart(i));

    bulkCreateFinancingPayments(propertyId, financingId, { items })
      .then((results) => {
        let hasErrors = false;
        for (const result of results) {
          if (result.error) {
            hasErrors = true;
            callbacks.onRowError(result.index ?? 0, result.error);
          } else {
            callbacks.onRowSuccess(result.index ?? 0);
          }
        }

        queryClient.invalidateQueries({
          queryKey: ['financingPayments', propertyId],
        });
        queryClient.invalidateQueries({
          queryKey: ['propertyFinancials', propertyId],
        });
        queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });

        setIsSubmitting(false);
        callbacks.onComplete();

        const successCount = results.filter((r) => !r.error).length;
        if (successCount > 0) {
          showToast(
            `${successCount} payment${successCount > 1 ? 's' : ''} created successfully`,
            'success'
          );
        }
        if (!hasErrors) {
          onClose();
        }
      })
      .catch((e) => {
        rows.forEach((_, i) => callbacks.onRowError(i, getErrorMessage(e)));
        setIsSubmitting(false);
        callbacks.onComplete();
      });
  };

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
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-5xl w-full mx-4 max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between p-4 border-b border-border-strong flex-shrink-0">
          <h2 className="text-lg font-semibold text-text-primary">
            {t('financials.modals.bulkAddPayments')}
          </h2>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text-secondary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto p-4 flex-1">
          {/* Fixed fields */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
            <div>
              <label className={labelClass}>
                {t('financials.labels.currency')}
              </label>
              <CurrencySelector
                value={currency}
                onChange={setCurrency}
                disabled={isSubmitting}
              />
            </div>
            <div className="flex items-end pb-1">
              <label className="flex items-center gap-2 text-sm text-text-secondary">
                <input
                  type="checkbox"
                  checked={deductFromBalance}
                  onChange={(e) => setDeductFromBalance(e.target.checked)}
                  disabled={isSubmitting}
                  className="h-4 w-4 rounded border-border-strong text-primary-500 focus:ring-primary-500"
                />
                {t('financials.labels.deductFromBalance')}
              </label>
            </div>
          </div>

          <BulkDataGrid
            columns={BULK_COLUMNS}
            onSubmit={handleSubmit}
            isSubmitting={isSubmitting}
            disabled={!currency}
            dateFormat={
              defaultDateFormat as 'DD/MM/YYYY' | 'MM/DD/YYYY' | 'YYYY-MM-DD'
            }
          />
        </div>
      </div>
    </div>
  );
};
