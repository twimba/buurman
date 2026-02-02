import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ContractStatus } from '@/types/contract';
import { useContracts } from '@/hooks/useContractHooks';
import { ContractCard } from '@/components/contracts/ContractCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, FileText, Filter } from 'lucide-react';

const statusFilters = [
  { value: undefined, label: 'All Statuses' },
  { value: ContractStatus.ACTIVE, label: 'Active' },
  { value: ContractStatus.DRAFT, label: 'Draft' },
  { value: ContractStatus.PENDING_SIGNATURE, label: 'Pending Signature' },
  { value: ContractStatus.EXPIRED, label: 'Expired' },
  { value: ContractStatus.TERMINATED, label: 'Terminated' },
];

export const ContractsPage = () => {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<ContractStatus | undefined>(
    undefined
  );

  const {
    data: contracts,
    isLoading,
    error,
  } = useContracts(statusFilter ? { status: statusFilter } : undefined);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load contracts" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Contracts</h1>
          <button
            onClick={() => navigate('/contracts/new')}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Plus className="h-5 w-5" />
            Add Contract
          </button>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-white rounded-lg border border-gray-200 p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-gray-600" />
            <h2 className="font-semibold text-gray-900">Filters</h2>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Status
            </label>
            <div className="flex gap-2 flex-wrap">
              {statusFilters.map((filter) => (
                <button
                  key={filter.label}
                  onClick={() => setStatusFilter(filter.value)}
                  className={`px-4 py-2 rounded transition-colors text-sm ${
                    statusFilter === filter.value
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Contract Count */}
        <p className="text-sm text-gray-600 mb-4">
          {contracts?.length || 0}{' '}
          {contracts?.length === 1 ? 'contract' : 'contracts'}
        </p>

        {/* Contracts Grid */}
        {contracts && contracts.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {contracts.map((contract) => (
              <ContractCard key={contract.id} contract={contract} />
            ))}
          </div>
        ) : (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-white rounded-lg">
            <FileText className="h-16 w-16 text-gray-300 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No contracts yet
            </h3>
            <p className="text-gray-600 mb-6">
              Get started by creating your first rental agreement
            </p>
            <button
              onClick={() => navigate('/contracts/new')}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <Plus className="h-5 w-5" />
              Add Contract
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
