import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ContractStatus } from '@/types/contract';
import { useContracts } from '@/hooks/useContractHooks';
import { ContractCard } from '@/components/contracts/ContractCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, FileText, Filter } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { Pagination } from '@/components/ui/Pagination';
import { RefreshButton } from '@/components/ui/RefreshButton';

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
  const { canEditData } = useTeam();
  const [statusFilter, setStatusFilter] = useState<ContractStatus | undefined>(
    undefined
  );
  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    resetPage,
  } = usePagination({ defaultSize: 12 });

  const {
    data: contractsData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = useContracts(
    statusFilter ? { status: statusFilter, ...pageParams } : { ...pageParams }
  );
  const contracts = contractsData?.content;

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
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <FileText className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Contracts
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
              Manage rental agreements and lease terms
            </p>
          </div>
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <button
              onClick={() => navigate('/contracts/new')}
              disabled={!canEditData}
              className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
            >
              <Plus className="h-5 w-5" />
              Add Contract
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
            <h2 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Filters
            </h2>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
              Status
            </label>
            <div className="flex gap-2 flex-wrap">
              {statusFilters.map((filter) => (
                <button
                  key={filter.label}
                  onClick={() => {
                    setStatusFilter(filter.value);
                    resetPage();
                  }}
                  className={`px-4 py-2 rounded transition-colors text-sm ${
                    statusFilter === filter.value
                      ? 'bg-[#5c7cfa] text-white'
                      : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54]'
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Contract Count */}
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
          {contractsData?.totalElements ?? 0}{' '}
          {contractsData?.totalElements === 1 ? 'contract' : 'contracts'}
        </p>

        {/* Contracts Grid */}
        {contracts && contracts.length > 0 ? (
          <>
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {contracts.map((contract) => (
                <ContractCard key={contract.identifier} contract={contract} />
              ))}
            </div>
            {contractsData && (
              <div className="mt-6">
                <Pagination
                  page={page}
                  totalPages={contractsData.totalPages}
                  totalElements={contractsData.totalElements}
                  size={size}
                  onPageChange={handlePageChange}
                  onSizeChange={handleSizeChange}
                />
              </div>
            )}
          </>
        ) : (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-white dark:bg-[#14161f] rounded-lg">
            <FileText className="h-16 w-16 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No contracts yet
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
              Get started by creating your first rental agreement
            </p>
            <button
              onClick={() => navigate('/contracts/new')}
              disabled={!canEditData}
              className="bg-[#5c7cfa] text-white px-6 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
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
