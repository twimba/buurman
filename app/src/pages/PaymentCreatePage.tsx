import { useState, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getContracts } from '@/api/contracts';
import { useCreatePayment } from '@/hooks/usePaymentHooks';
import { PaymentForm } from '@/components/payments/PaymentForm';
import { RegisterPaymentForm } from '@/components/payments/RegisterPaymentForm';
import { ContractSelector } from '@/components/common/ContractSelector';
import { CreatePaymentRequest } from '@/types/payment';
import { ArrowLeft, AlertTriangle } from 'lucide-react';

export const PaymentCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createPaymentMutation = useCreatePayment();

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

  // Effective contract ID: clear if prefill is invalid and user hasn't picked a new one
  const effectiveContractId =
    isPrefillInvalid && selectedContractId === prefilledContractId
      ? ''
      : selectedContractId;

  const handleSubmit = async (data: CreatePaymentRequest) => {
    await createPaymentMutation.mutateAsync(data);
    navigate('/payments');
  };

  const handleCancel = () => {
    navigate('/payments');
  };

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
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            {registerMode ? 'Register Payment' : 'Schedule Payment'}
          </h1>
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
              disabled={createPaymentMutation.isPending}
            />
            {!effectiveContractId && !showWarning && (
              <p className="mt-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Please select a contract first
              </p>
            )}
          </div>

          {effectiveContractId &&
            (registerMode ? (
              <RegisterPaymentForm
                onSubmit={handleSubmit}
                onCancel={handleCancel}
                isLoading={createPaymentMutation.isPending}
                contractIdentifier={effectiveContractId}
              />
            ) : (
              <PaymentForm
                onSubmit={handleSubmit}
                onCancel={handleCancel}
                isLoading={createPaymentMutation.isPending}
                contractIdentifier={effectiveContractId}
              />
            ))}
        </div>
      </div>
    </div>
  );
};
