import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useContract, useUpdateContract } from '@/hooks/useContractHooks';
import { ContractForm } from '@/components/contracts/ContractForm';
import { CreateContractRequest, UpdateContractRequest } from '@/types/contract';
import { LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const ContractEditPage = () => {
  const { t } = useTranslation('contracts');
  const { id = '' } = useParams<{ id: string }>();
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
      <div className="min-h-screen bg-surface-page flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !contract) {
    return (
      <div className="min-h-screen bg-surface-page p-8">
        <ErrorMessage message={t('edit.failedToLoad')} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/contracts/${id}`)}
            className="p-2 hover:bg-neutral-100 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">
            {t('edit.title')}
          </h1>
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
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
