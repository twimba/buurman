import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreatePayment } from '@/hooks/usePaymentHooks';
import { PaymentForm } from '@/components/payments/PaymentForm';
import { ContractSelector } from '@/components/common/ContractSelector';
import { CreatePaymentRequest } from '@/types/payment';
import { ArrowLeft } from 'lucide-react';

export const PaymentCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createPaymentMutation = useCreatePayment();

  const prefilledContractId = searchParams.get('contractId') || '';
  const [selectedContractId, setSelectedContractId] =
    useState(prefilledContractId);

  const handleSubmit = async (data: CreatePaymentRequest) => {
    await createPaymentMutation.mutateAsync(data);
    navigate('/payments');
  };

  const handleCancel = () => {
    navigate('/payments');
  };

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
            Add New Payment
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
          <div className="mb-6">
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
              Contract <span className="text-red-500">*</span>
            </label>
            <ContractSelector
              value={selectedContractId}
              onChange={setSelectedContractId}
              disabled={createPaymentMutation.isPending}
            />
            {!selectedContractId && (
              <p className="mt-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Please select a contract first
              </p>
            )}
          </div>

          {selectedContractId && (
            <PaymentForm
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              isLoading={createPaymentMutation.isPending}
              contractId={selectedContractId}
            />
          )}
        </div>
      </div>
    </div>
  );
};
