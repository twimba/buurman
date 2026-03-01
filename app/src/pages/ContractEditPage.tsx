import { useNavigate, useParams } from 'react-router-dom';
import { useContract, useUpdateContract } from '@/hooks/useContractHooks';
import { ContractForm } from '@/components/contracts/ContractForm';
import { CreateContractRequest, UpdateContractRequest } from '@/types/contract';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const ContractEditPage = () => {
  const { id = "" } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: contract, isLoading, error } = useContract(id);
  const updateContractMutation = useUpdateContract(id);

  const handleSubmit = async (data: CreateContractRequest) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { parties, ...updateData } = data;
    await updateContractMutation.mutateAsync(
      updateData as UpdateContractRequest
    );
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14] flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !contract) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14] p-8">
        <ErrorMessage message="Failed to load contract" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14]">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/contracts/${id}`)}
            className="p-2 hover:bg-[#e8ecf4] dark:bg-[#1e2130] rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Edit Contract
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
          <ContractForm
            contract={contract}
            onSubmit={handleSubmit}
            isLoading={updateContractMutation.isPending}
          />
        </div>
      </div>
    </div>
  );
};
