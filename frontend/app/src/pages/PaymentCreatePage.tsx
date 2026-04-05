import { useState, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getContracts } from '@/api/contracts';
import { bulkCreatePayments } from '@/api/payments';
import { useCreatePayment } from '@/hooks/usePaymentHooks';
import { PaymentForm } from '@/components/payments/PaymentForm';
import { RegisterPaymentForm } from '@/components/payments/RegisterPaymentForm';
import { ContractSelector } from '@/components/common/ContractSelector';
import {
  BulkDataGrid,
  type BulkColumnDef,
  type RowData,
} from '@/components/common/BulkDataGrid';
import { CurrencySelector } from '@/components/common/CurrencySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { CreatePaymentRequest } from '@/types/payment';
import { ArrowLeft, AlertTriangle, CheckCircle } from 'lucide-react';
import { getErrorMessage } from '@/utils/errorMessages';

type Mode = 'single' | 'bulk';

const BULK_COLUMNS: BulkColumnDef[] = [
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
  const { t } = useTranslation('payments');
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const createPaymentMutation = useCreatePayment();
  const { defaultCurrency, defaultDateFormat } = useTeamDefaults();

  const prefilledContractId = searchParams.get('contractId') ?? '';
  const registerMode = searchParams.get('register') === 'true';

  const { data: activeContracts } = useQuery({
    queryKey: ['contracts', 'ACTIVE'],
    queryFn: () => getContracts({ status: 'ACTIVE' }),
  });

  const isPrefillInvalid = useMemo(() => {
    if (!prefilledContractId || !activeContracts) {
      return false;
    }
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
      navigate(-1);
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  const handleBulkSubmit = (
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

    const items: CreatePaymentRequest[] = rows.map((row) => ({
      contractIdentifier: effectiveContractId,
      amount: parseFloat(row.amount),
      currency,
      dueDate: row.date,
      markAsPaid: registerMode ? true : undefined,
      paymentDate: registerMode ? row.date : undefined,
    }));

    // Mark all rows as submitting
    rows.forEach((_, i) => callbacks.onRowStart(i));

    bulkCreatePayments(items)
      .then((results) => {
        let hasErrors = false;
        for (const result of results) {
          if (result.error) {
            hasErrors = true;
            callbacks.onRowError(result.index, result.error);
          } else {
            callbacks.onRowSuccess(result.index);
          }
        }

        queryClient.invalidateQueries({ queryKey: ['payments'] });
        queryClient.invalidateQueries({ queryKey: ['paymentStats'] });
        queryClient.invalidateQueries({ queryKey: ['contracts'] });
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
        queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
        queryClient.invalidateQueries({ queryKey: ['income-trend'] });

        setBulkSubmitting(false);
        callbacks.onComplete();
        if (!hasErrors) {
          navigate(-1);
        }
      })
      .catch((e) => {
        rows.forEach((_, i) => callbacks.onRowError(i, getErrorMessage(e)));
        setBulkSubmitting(false);
        callbacks.onComplete();
      });
  };

  const showWarning = isPrefillInvalid && !dismissedWarning;

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-neutral-100 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-text-primary">
              {registerMode ? t('create.titleRegister') : t('create.titleSchedule')}
            </h1>
            {addedCount > 0 && (
              <p className="flex items-center gap-1.5 text-sm text-success-text mt-1">
                <CheckCircle className="h-3.5 w-3.5" />
                {t('create.addedCount', {
                  count: addedCount,
                  action: registerMode ? t('create.actionRegistered') : t('create.actionScheduled'),
                })}
              </p>
            )}
          </div>
        </div>

        {/* Mode toggle */}
        <div className="flex gap-1 mb-4 p-1 bg-neutral-100 rounded-lg w-fit">
          {(['single', 'bulk'] as Mode[]).map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => setMode(m)}
              className={`px-4 py-1.5 text-sm font-medium rounded-md transition-colors ${
                mode === m
                  ? 'bg-surface-card text-text-primary shadow-sm'
                  : 'text-text-secondary hover:text-text-secondary'
              }`}
            >
              {m === 'single' ? t('create.single') : t('create.bulk')}
            </button>
          ))}
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          {showWarning && (
            <div className="mb-4 flex items-start gap-2 p-3 bg-warning-bg border border-warning-border rounded text-sm text-warning-text">
              <AlertTriangle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span>
                {t('create.contractInactiveWarning')}
              </span>
            </div>
          )}

          <div className="mb-6">
            <label className="block text-sm font-medium text-text-secondary mb-2">
              {t('form.contract')} <span className="text-error-text">*</span>
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
              <p className="mt-1 text-sm text-text-secondary">
                {t('form.selectContractFirst')}
              </p>
            )}
          </div>

          {effectiveContractId && mode === 'single' && (
            <>
              {registerMode ? (
                <RegisterPaymentForm
                  onSubmit={handleSubmit}
                  onCancel={handleCancel}
                  isLoading={createPaymentMutation.isPending}
                  contractIdentifier={effectiveContractId}
                  resetKey={resetKey}
                  continueAdding={continueAdding}
                  onContinueAddingChange={setContinueAdding}
                />
              ) : (
                <PaymentForm
                  onSubmit={handleSubmit}
                  onCancel={handleCancel}
                  isLoading={createPaymentMutation.isPending}
                  contractIdentifier={effectiveContractId}
                  resetKey={resetKey}
                  continueAdding={continueAdding}
                  onContinueAddingChange={setContinueAdding}
                />
              )}
            </>
          )}

          {effectiveContractId && mode === 'bulk' && (
            <>
              {/* Currency selector for bulk */}
              <div className="mb-6">
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('form.currency')} <span className="text-error-text">*</span>
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
                disabled={
                  !effectiveContractId || !(bulkCurrency || defaultCurrency)
                }
                dateFormat={
                  defaultDateFormat as
                    | 'DD/MM/YYYY'
                    | 'MM/DD/YYYY'
                    | 'YYYY-MM-DD'
                }
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
};
