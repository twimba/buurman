import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreateContract } from '@/hooks/useContractHooks';
import { ContractForm } from '@/components/contracts/ContractForm';
import { CreateContractRequest } from '@/types/contract';
import { ArrowLeft } from 'lucide-react';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';

export const ContractCreatePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createContractMutation = useCreateContract();

  const prefilledPropertyId = searchParams.get('propertyId') ?? undefined;
  const prefilledContactId = searchParams.get('contactId') ?? undefined;

  const handleSubmit = async (data: CreateContractRequest) => {
    await createContractMutation.mutateAsync(data);
  };

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/contracts')}
            className="p-2 hover:bg-neutral-100 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">
            Add New Contract
          </h1>
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <ImpersonationGuard
            blockInReadOnly
            fallback={
              <div className="text-center py-8 text-text-secondary">
                Creating contracts is not available in read-only mode.
              </div>
            }
          >
            <ContractForm
              onSubmit={handleSubmit}
              isLoading={createContractMutation.isPending}
              prefilledPropertyId={prefilledPropertyId}
              prefilledContactId={prefilledContactId}
            />
          </ImpersonationGuard>
        </div>
      </div>
    </div>
  );
};
