import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreateContract } from '@/hooks/useContractHooks';
import { ContractForm } from '@/components/contracts/ContractForm';
import { CreateContractRequest } from '@/types/contract';
import { ArrowLeft } from 'lucide-react';

export const ContractCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createContractMutation = useCreateContract();

  const prefilledPropertyId = searchParams.get('propertyId') || undefined;
  const prefilledTenantId = searchParams.get('tenantId') || undefined;

  const handleSubmit = async (data: CreateContractRequest) => {
    await createContractMutation.mutateAsync(data);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/contracts')}
            className="p-2 hover:bg-gray-200 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-gray-900">Add New Contract</h1>
        </div>

        {/* Form */}
        <div className="bg-white rounded-lg shadow p-6">
          <ContractForm
            onSubmit={handleSubmit}
            isLoading={createContractMutation.isPending}
            prefilledPropertyId={prefilledPropertyId}
            prefilledTenantId={prefilledTenantId}
          />
        </div>
      </div>
    </div>
  );
};
