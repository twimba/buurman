import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ContractStatus } from '@/types/contract';
import { useContracts } from '@/hooks/useContractHooks';
import { useDebounce } from '@/hooks/useDebounce';
import { ContractCard } from '@/components/contracts/ContractCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  FileText,
  RefreshCw,
  CircleDot,
  X,
  Search,
  ArrowUp,
  ArrowDown,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import {
  EmptyState,
  FilterSelectPopover,
  ListPageHeader,
  Pagination,
  RefreshButton,
  Skeleton,
  type ListPageHeaderAction,
} from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { EntityExportControls } from '@/components/common/EntityExportControls';
import { SavedFiltersDropdown } from '@/components/contracts/SavedFiltersDropdown';
import {
  exportContractsCsv,
  exportContractsXlsx,
  exportContractsGoogleSheet,
  exportDepositsCsv,
  exportDepositsXlsx,
  exportDepositsGoogleSheet,
} from '@/generated/api/booklets/booklets';
import { GOOGLE_SHEET_EXPORT_TIMEOUT_MS } from '@/utils/googleSheetExport';

export const ContractsPage = () => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();
  const { canEditData } = useTeam();

  const statusOptions = useMemo(
    () => [
      { value: ContractStatus.ACTIVE, label: t('list.active') },
      { value: ContractStatus.DRAFT, label: t('list.draft') },
      {
        value: ContractStatus.PENDING_SIGNATURE,
        label: t('list.pendingSignature'),
      },
      { value: ContractStatus.EXPIRED, label: t('list.expired') },
      { value: ContractStatus.TERMINATED, label: t('list.terminated') },
      { value: ContractStatus.NOTICE_GIVEN, label: t('list.noticeGiven') },
    ],
    [t]
  );
  const [statusFilter, setStatusFilter] = useState<ContractStatus | undefined>(
    undefined
  );
  const [searchInput, setSearchInput] = useState('');
  const debouncedSearch = useDebounce(searchInput, 300);
  const [endingWithinDays, setEndingWithinDays] = useState<
    number | undefined
  >();
  const [sortField, setSortField] = useState<
    'endDate' | 'startDate' | 'rentAmount'
  >('endDate');
  const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('ASC');

  const sortOptions = useMemo(
    () =>
      [
        { value: 'endDate' as const, label: t('list.sort.endDate') },
        { value: 'startDate' as const, label: t('list.sort.startDate') },
        { value: 'rentAmount' as const, label: t('list.sort.rentAmount') },
      ] as const,
    [t]
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
  } = useContracts({
    ...(statusFilter ? { status: statusFilter } : {}),
    ...pageParams,
    search: debouncedSearch || undefined,
    endingWithinDays,
    sort: sortField,
    direction: sortDirection,
  });
  const contracts = contractsData?.content;

  const validStatuses: string[] = Object.values(ContractStatus);
  const validSortFields = ['endDate', 'startDate', 'rentAmount'] as const;

  const handleApplySavedFilter = (criteria: Record<string, unknown>) => {
    const {
      status,
      search,
      endingWithinDays: days,
      sort,
      direction,
    } = criteria;

    setStatusFilter(
      typeof status === 'string' && validStatuses.includes(status)
        ? (status as ContractStatus)
        : undefined
    );
    setSearchInput(typeof search === 'string' ? search : '');
    setEndingWithinDays(typeof days === 'number' ? days : undefined);
    setSortField(
      typeof sort === 'string' &&
        (validSortFields as readonly string[]).includes(sort)
        ? (sort as (typeof validSortFields)[number])
        : 'endDate'
    );
    setSortDirection(direction === 'DESC' ? 'DESC' : 'ASC');
    resetPage();
  };

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
            csv={() =>
              exportContractsCsv({
                status: statusFilter,
                search: debouncedSearch || undefined,
                endingWithinDays,
              })
            }
            xlsx={() =>
              exportContractsXlsx({
                status: statusFilter,
                search: debouncedSearch || undefined,
                endingWithinDays,
              })
            }
            googleSheet={(accessToken) =>
              exportContractsGoogleSheet(
                { accessToken },
                { timeout: GOOGLE_SHEET_EXPORT_TIMEOUT_MS }
              )
            }
          />
          <EntityExportControls
            filenameStem="deposits"
            label={t('list.exportDeposits')}
            csv={() => exportDepositsCsv()}
            xlsx={() => exportDepositsXlsx()}
            googleSheet={(accessToken) =>
              exportDepositsGoogleSheet(
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

        {/* Toolbar — search, status filter, ending-within-days, sort */}
        <div className="flex flex-wrap items-center gap-2 mb-4">
          <div className="relative flex-1 min-w-[220px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => {
                setSearchInput(e.target.value);
                resetPage();
              }}
              placeholder={t('list.searchPlaceholder')}
              aria-label={t('list.searchPlaceholder')}
              className="w-full h-10 pl-10 pr-9 border border-border-strong rounded-lg focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
            />
            {searchInput && (
              <button
                onClick={() => {
                  setSearchInput('');
                  resetPage();
                }}
                aria-label={t('common:buttons.clear', 'Clear')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 p-0.5 rounded text-text-muted hover:text-text-secondary focus-ring"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          <FilterSelectPopover
            icon={CircleDot}
            label={t('list.status')}
            options={statusOptions}
            value={statusFilter}
            onChange={(value) => {
              setStatusFilter(value);
              resetPage();
            }}
            allLabel={t('list.allStatuses')}
          />

          <input
            type="number"
            min={0}
            value={endingWithinDays ?? ''}
            onChange={(e) => {
              const raw = e.target.value;
              setEndingWithinDays(raw === '' ? undefined : Number(raw));
              resetPage();
            }}
            placeholder={t('list.endingWithinDays')}
            aria-label={t('list.endingWithinDays')}
            className="h-10 w-40 px-3 border border-border-strong rounded-lg focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
          />

          {/* Sort — grouped control pinned to the same 40px baseline */}
          <div className="inline-flex items-center h-10 rounded-lg border border-border-strong bg-surface-card">
            <select
              value={sortField}
              onChange={(e) => {
                setSortField(
                  e.target.value as 'endDate' | 'startDate' | 'rentAmount'
                );
                resetPage();
              }}
              aria-label={t('list.sortLabel')}
              className="h-full bg-transparent pl-3 pr-2 text-sm font-medium text-text-secondary rounded-l-lg focus-ring"
            >
              {sortOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <span aria-hidden className="w-px h-5 bg-border-default" />
            <button
              onClick={() => {
                setSortDirection((d) => (d === 'ASC' ? 'DESC' : 'ASC'));
                resetPage();
              }}
              className="h-10 w-10 inline-flex items-center justify-center rounded-r-lg text-text-secondary hover:text-primary-600 hover:bg-surface-inset transition-colors focus-ring"
              title={
                sortDirection === 'ASC' ? t('list.sortAsc') : t('list.sortDesc')
              }
              aria-label={
                sortDirection === 'ASC' ? t('list.sortAsc') : t('list.sortDesc')
              }
            >
              {sortDirection === 'ASC' ? (
                <ArrowUp className="h-4 w-4" />
              ) : (
                <ArrowDown className="h-4 w-4" />
              )}
            </button>
          </div>

          <SavedFiltersDropdown
            currentCriteria={{
              status: statusFilter,
              search: debouncedSearch,
              endingWithinDays,
              sort: sortField,
              direction: sortDirection,
            }}
            onApply={handleApplySavedFilter}
          />
        </div>

        {/* Active filter chips */}
        {(statusFilter ||
          debouncedSearch ||
          endingWithinDays !== undefined) && (
          <div className="flex flex-wrap items-center gap-2 mb-4">
            {statusFilter && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300">
                {statusOptions.find((o) => o.value === statusFilter)?.label}
                <button
                  onClick={() => {
                    setStatusFilter(undefined);
                    resetPage();
                  }}
                  aria-label={t('common:buttons.clear', 'Clear')}
                  className="rounded-full hover:text-primary-900 focus-ring"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
            {debouncedSearch && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-surface-inset text-text-secondary">
                {debouncedSearch}
                <button
                  onClick={() => {
                    setSearchInput('');
                    resetPage();
                  }}
                  aria-label={t('common:buttons.clear', 'Clear')}
                  className="rounded-full hover:text-text-primary focus-ring"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
            {endingWithinDays !== undefined && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-surface-inset text-text-secondary">
                {t('list.endingWithinDays')}: {endingWithinDays}
                <button
                  onClick={() => {
                    setEndingWithinDays(undefined);
                    resetPage();
                  }}
                  aria-label={t('common:buttons.clear', 'Clear')}
                  className="rounded-full hover:text-text-primary focus-ring"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
          </div>
        )}

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
