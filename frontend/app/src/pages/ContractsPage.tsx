import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ContractStatus } from '@/types/contract';
import { useContracts } from '@/hooks/useContractHooks';
import { ContractCard } from '@/components/contracts/ContractCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, FileText, Filter } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { Pagination, RefreshButton, Skeleton } from '@buurman/ui';

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
      <div className="min-h-screen bg-background">
        <div className="px-4 py-8 space-y-6">
          {/* Header skeleton */}
          <div className="flex justify-between items-center">
            <div className="space-y-2">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-4 w-64" />
            </div>
            <Skeleton className="h-10 w-36 rounded" />
          </div>
          {/* Filter bar skeleton */}
          <Skeleton className="h-24 w-full rounded-lg" />
          {/* Contract cards grid skeleton */}
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="p-4 rounded-lg border border-border-default space-y-3"
              >
                <div className="flex justify-between items-start">
                  <Skeleton className="h-5 w-2/3" />
                  <Skeleton className="h-6 w-16 rounded-full" />
                </div>
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
                <div className="flex gap-4 pt-2">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-24" />
                </div>
              </div>
            ))}
          </div>
        </div>
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
              <h1 className="text-3xl font-bold text-text-primary">
                Contracts
              </h1>
            </div>
            <p className="text-text-secondary ml-11">
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
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              Add Contract
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-surface-card rounded-lg border border-border-default p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h2 className="font-semibold text-text-primary">Filters</h2>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-2">
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
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Contract Count */}
        <p className="text-sm text-text-secondary mb-4">
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
          <div className="flex flex-col items-center justify-center py-16 bg-surface-card rounded-lg">
            <FileText className="h-16 w-16 text-text-disabled mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              No contracts yet
            </h3>
            <p className="text-text-secondary mb-6">
              Get started by creating your first rental agreement
            </p>
            <button
              onClick={() => navigate('/contracts/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
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
