import { useState, useMemo, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getContracts } from '@/api/contracts';
import { createPayment } from '@/api/payments';
import { useCreatePayment } from '@/hooks/usePaymentHooks';
import { PaymentForm } from '@/components/payments/PaymentForm';
import { RegisterPaymentForm } from '@/components/payments/RegisterPaymentForm';
import { ContractSelector } from '@/components/common/ContractSelector';
import {
  BulkDataGrid,
  type ColumnDef,
  type RowData,
} from '@/components/common/BulkDataGrid';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { CreatePaymentRequest } from '@/types/payment';
import { ArrowLeft, AlertTriangle, CheckCircle } from 'lucide-react';
import { getErrorMessage } from '@/utils/errorMessages';

type Mode = 'single' | 'bulk';

const BULK_COLUMNS: ColumnDef[] = [
  {
    key: 'date',
    label: 'Date',
    type: 'date',
    required: true,
    placeholder: 'YYYY-MM-DD',
  },
  {
    key: 'amount',
    label: 'Amount',
    type: 'number',
    required: true,
    placeholder: '0.00',
  },
];

export const PaymentCreatePage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const createPaymentMutation = useCreatePayment();
  const { defaultCurrency } = useTeamDefaults();

  const prefilledContractId = searchParams.get('contractId') || '';
  const registerMode = searchParams.get('register') === 'true';

  const { data: activeContracts } = useQuery({
    queryKey: ['contracts', 'ACTIVE'],
    queryFn: () => getContracts({ status: 'ACTIVE' }),
  });

  const isPrefillInvalid = useMemo(() => {
    if (!prefilledContractId || !activeContracts) return false;
    return !activeContracts.content?.some(
      (c) => c.identifier === prefilledContractId
    );
  }, [prefilledContractId, activeContracts]);

  const [selectedContractId, setSelectedContractId] =
    useState(prefilledContractId);
  const [dismissedWarning, setDismissedWarning] = useState(false);
  const [continueAdding, setContinueAdding] = useState(false);
  const [resetKey, setResetKey] = useState(0);
  const [addedCount, setAddedCount] = useState(0);
  const [mode, setMode] = useState<Mode>('single');
  const [bulkCurrency, setBulkCurrency] = useState(defaultCurrency || '');
  const [bulkSubmitting, setBulkSubmitting] = useState(false);

  const effectiveContractId =
    isPrefillInvalid && selectedContractId === prefilledContractId
      ? ''
      : selectedContractId;

  const handleSubmit = async (data: CreatePaymentRequest) => {
    await createPaymentMutation.mutateAsync(data);
    if (continueAdding) {
      setAddedCount((c) => c + 1);
      setResetKey((k) => k + 1);
    } else {
      navigate('/payments');
    }
  };

  const handleCancel = () => {
    navigate('/payments');
  };

  const handleBulkSubmit = useCallback(
    (
      rows: RowData[],
      callbacks: {
        onRowStart: (index: number) => void;
        onRowSuccess: (index: number) => void;
        onRowError: (index: number, error: string) => void;
        onComplete: () => void;
      }
    ) => {
      const currency = bulkCurrency || defaultCurrency || '';
      setBulkSubmitting(true);

      (async () => {
        for (let i = 0; i < rows.length; i++) {
          callbacks.onRowStart(i);
          try {
            const req: CreatePaymentRequest = {
              contractIdentifier: effectiveContractId,
              amount: parseFloat(rows[i].amount),
              currency,
              dueDate: rows[i].date,
              markAsPaid: registerMode ? true : undefined,
            };
            await createPayment(req);
            callbacks.onRowSuccess(i);
          } catch (e) {
            callbacks.onRowError(i, getErrorMessage(e));
          }
        }

        // Invalidate caches once
        queryClient.invalidateQueries({ queryKey: ['payments'] });
        queryClient.invalidateQueries({ queryKey: ['paymentStats'] });
        queryClient.invalidateQueries({ queryKey: ['contracts'] });
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
        queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
        queryClient.invalidateQueries({ queryKey: ['income-trend'] });

        setBulkSubmitting(false);
        callbacks.onComplete();
      })();
    },
    [
      effectiveContractId,
      bulkCurrency,
      defaultCurrency,
      registerMode,
      queryClient,
    ]
  );

  const showWarning = isPrefillInvalid && !dismissedWarning;

  return (
    <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14]">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/payments')}
            className="p-2 hover:bg-[#e8ecf4] dark:bg-[#1e2130] rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              {registerMode ? 'Register Payment' : 'Schedule Payment'}
            </h1>
            {addedCount > 0 && (
              <p className="flex items-center gap-1.5 text-sm text-emerald-600 dark:text-emerald-400 mt-1">
                <CheckCircle className="h-3.5 w-3.5" />
                {addedCount} payment{addedCount !== 1 ? 's' : ''}{' '}
                {registerMode ? 'registered' : 'scheduled'} this session
              </p>
            )}
          </div>
        </div>

        {/* Mode toggle */}
        <div className="flex gap-1 mb-4 p-1 bg-[#e8ecf4] dark:bg-[#1e2130] rounded-lg w-fit">
          {(['single', 'bulk'] as Mode[]).map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => setMode(m)}
              className={`px-4 py-1.5 text-sm font-medium rounded-md transition-colors ${
                mode === m
                  ? 'bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]'
              }`}
            >
              {m === 'single' ? 'Single' : 'Bulk'}
            </button>
          ))}
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
          {showWarning && (
            <div className="mb-4 flex items-start gap-2 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded text-sm text-amber-700 dark:text-amber-300">
              <AlertTriangle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span>
                The linked contract is not active. Please select an active
                contract.
              </span>
            </div>
          )}

          <div className="mb-6">
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
              Contract <span className="text-red-500">*</span>
            </label>
            <ContractSelector
              value={effectiveContractId}
              onChange={(id) => {
                setSelectedContractId(id);
                setDismissedWarning(true);
              }}
              disabled={createPaymentMutation.isPending || bulkSubmitting}
            />
            {!effectiveContractId && !showWarning && (
              <p className="mt-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Please select a contract first
              </p>
            )}
          </div>

          {effectiveContractId && mode === 'single' && (
            <>
              {/* Continue adding checkbox */}
              <label className="flex items-center gap-2 mb-6 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={continueAdding}
                  onChange={(e) => setContinueAdding(e.target.checked)}
                  className="h-4 w-4 rounded border-[#c9cfd9] dark:border-[#3a3f54] text-[#5c7cfa] focus:ring-[#5c7cfa]"
                />
                <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                  Continue adding more
                </span>
              </label>

              {registerMode ? (
                <RegisterPaymentForm
                  onSubmit={handleSubmit}
                  onCancel={handleCancel}
                  isLoading={createPaymentMutation.isPending}
                  contractIdentifier={effectiveContractId}
                  resetKey={resetKey}
                />
              ) : (
                <PaymentForm
                  onSubmit={handleSubmit}
                  onCancel={handleCancel}
                  isLoading={createPaymentMutation.isPending}
                  contractIdentifier={effectiveContractId}
                  resetKey={resetKey}
                />
              )}
            </>
          )}

          {effectiveContractId && mode === 'bulk' && (
            <>
              {/* Currency selector for bulk */}
              <div className="mb-6">
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Currency <span className="text-red-500">*</span>
                </label>
                <CurrencySelector
                  value={bulkCurrency || defaultCurrency || ''}
                  onChange={setBulkCurrency}
                  disabled={bulkSubmitting}
                />
              </div>

              <BulkDataGrid
                columns={BULK_COLUMNS}
                onSubmit={handleBulkSubmit}
                isSubmitting={bulkSubmitting}
                disabled={!effectiveContractId || !(bulkCurrency || defaultCurrency)}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
};
