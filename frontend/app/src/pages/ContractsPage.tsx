import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ContractStatus } from '@/types/contract';
import { useContracts } from '@/hooks/useContractHooks';
import { ContractCard } from '@/components/contracts/ContractCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, FileText, Filter, RefreshCw } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import {
  EmptyState,
  FilterSheet,
  ListPageHeader,
  Pagination,
  RefreshButton,
  Skeleton,
  type ListPageHeaderAction,
} from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { EntityExportControls } from '@/components/common/EntityExportControls';
import {
  exportContractsCsv,
  exportContractsXlsx,
  exportContractsGoogleSheet,
} from '@/generated/api/booklets/booklets';
import { GOOGLE_SHEET_EXPORT_TIMEOUT_MS } from '@/utils/googleSheetExport';

export const ContractsPage = () => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();
  const { canEditData } = useTeam();

  const statusFilters = useMemo(
    () => [
      { value: undefined, label: t('list.allStatuses') },
      { value: ContractStatus.ACTIVE, label: t('list.active') },
      { value: ContractStatus.DRAFT, label: t('list.draft') },
      {
        value: ContractStatus.PENDING_SIGNATURE,
        label: t('list.pendingSignature'),
      },
      { value: ContractStatus.EXPIRED, label: t('list.expired') },
      { value: ContractStatus.TERMINATED, label: t('list.terminated') },
    ],
    [t]
  );
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
      <div className="min-h-full bg-background">
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
      <div className="min-h-full bg-background p-8">
        <ErrorMessage message={t('list.error')} />
      </div>
    );
  }

  const headerActions: ListPageHeaderAction[] = [
    {
      label: t('common:refresh', 'Refresh'),
      icon: RefreshCw,
      onClick: () => refetch(),
      showOn: 'mobile',
    },
    {
      label: 'desktop-actions',
      showOn: 'desktop',
      render: () => (
        <div className="flex items-center gap-2">
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
          <EntityExportControls
            filenameStem="contracts"
            csv={() => exportContractsCsv()}
            xlsx={() => exportContractsXlsx()}
            googleSheet={(accessToken) =>
              exportContractsGoogleSheet(
                { accessToken },
                { timeout: GOOGLE_SHEET_EXPORT_TIMEOUT_MS }
              )
            }
          />
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-4 md:py-8">
        <ListPageHeader
          title={t('list.title')}
          subtitle={t('list.subtitle')}
          icon={FileText}
          mobileLeading={<MobileMenuButton />}
          actions={headerActions}
          primaryAction={{
            label: t('list.addButton'),
            icon: Plus,
            onClick: () => navigate('/contracts/new'),
            disabled: !canEditData,
          }}
        />

        {/* Phone: search-less trigger + sheet. md+: inline filter card. */}
        <div className="md:hidden mb-4 flex items-center justify-end">
          <FilterSheet
            activeCount={statusFilter ? 1 : 0}
            onClear={() => {
              setStatusFilter(undefined);
              resetPage();
            }}
            triggerLabel={t('list.filters')}
            collapseBelow="lg"
          >
            <ContractsStatusFilterContent
              statusFilters={statusFilters}
              statusFilter={statusFilter}
              setStatusFilter={setStatusFilter}
              resetPage={resetPage}
              t={t}
            />
          </FilterSheet>
        </div>

        {/* Filter Bar (md+) */}
        <div className="hidden md:block mb-6 bg-surface-card rounded-lg border border-border-default p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h2 className="font-semibold text-text-primary">
              {t('list.filters')}
            </h2>
          </div>

          <ContractsStatusFilterContent
            statusFilters={statusFilters}
            statusFilter={statusFilter}
            setStatusFilter={setStatusFilter}
            resetPage={resetPage}
            t={t}
          />
        </div>

        {/* Contract Count */}
        <p className="text-sm text-text-secondary mb-4">
          {t('list.count', { count: contractsData?.totalElements ?? 0 })}
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
          <div className="bg-surface-card rounded-lg">
            <EmptyState
              variant="page"
              icon={<FileText className="h-12 w-12" />}
              title={t('list.empty.title')}
              description={t('list.empty.description')}
              actions={
                <button
                  onClick={() => navigate('/contracts/new')}
                  disabled={!canEditData}
                  className="bg-primary-500 text-white px-6 py-2 rounded min-h-touch hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500 focus-ring"
                >
                  <Plus className="h-5 w-5" />
                  {t('list.addButton')}
                </button>
              }
            />
          </div>
        )}
      </div>
    </div>
  );
};

interface ContractsStatusFilterContentProps {
  statusFilters: { label: string; value: ContractStatus | undefined }[];
  statusFilter: ContractStatus | undefined;
  setStatusFilter: (value: ContractStatus | undefined) => void;
  resetPage: () => void;
  t: (key: string) => string;
}

const ContractsStatusFilterContent = ({
  statusFilters,
  statusFilter,
  setStatusFilter,
  resetPage,
  t,
}: ContractsStatusFilterContentProps) => (
  <div>
    <label className="block text-sm font-medium text-text-secondary mb-2">
      {t('list.status')}
    </label>
    <div className="flex gap-2 flex-wrap">
      {statusFilters.map((filter) => (
        <button
          key={filter.label}
          onClick={() => {
            setStatusFilter(filter.value);
            resetPage();
          }}
          className={`px-4 py-2 rounded transition-colors text-sm min-h-touch ${
            statusFilter === filter.value
              ? 'bg-primary-500 text-white'
              : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
          }`}
        >
          {filter.label}
        </button>
      ))}
    </div>
  </div>
);
