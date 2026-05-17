import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PropertyStatus, PropertyCategory } from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { useProperties } from '@/hooks/usePropertyHooks';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home, Filter, Search, X } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { useDebounce } from '@/hooks/useDebounce';
import {
  EmptyState,
  FilterSheet,
  ListPageHeader,
  Pagination,
  RefreshButton,
  Skeleton,
  type ListPageHeaderAction,
} from '@buurman/ui';
import { EntityExportControls } from '@/components/common/EntityExportControls';
import { exportPropertiesCsv, exportPropertiesXlsx } from '@/api/listExports';
import { exportPropertiesGoogleSheet } from '@/api/googleSheetsExport';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { RefreshCw } from 'lucide-react';

export const PropertyListPage = () => {
  const { t } = useTranslation('properties');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { statusLabel, categoryLabel } = usePropertyLabels();

  const categoryFilters = useMemo<
    { value: PropertyCategory | undefined; label: string }[]
  >(
    () => [
      { value: undefined, label: t('list.allCategories') },
      ...Object.values(PropertyCategory).map((cat) => ({
        value: cat,
        label: categoryLabel(cat),
      })),
    ],
    [t, categoryLabel]
  );

  const statusFilters = useMemo<
    { value: PropertyStatus | undefined; label: string }[]
  >(
    () => [
      { value: undefined, label: t('list.allStatuses') },
      ...Object.values(PropertyStatus).map((status) => ({
        value: status,
        label: statusLabel(status),
      })),
    ],
    [t, statusLabel]
  );
  const [statusFilter, setStatusFilter] = useState<PropertyStatus | undefined>(
    undefined
  );
  const [categoryFilter, setCategoryFilter] = useState<
    PropertyCategory | undefined
  >(undefined);
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedQuery = useDebounce(searchQuery, 300);

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    resetPage,
  } = usePagination({ defaultSize: 12 });

  const {
    data: propertiesData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = useProperties({
    status: statusFilter,
    category: categoryFilter,
    query: debouncedQuery || undefined,
    ...pageParams,
  });

  const properties = propertiesData?.content ?? [];

  if (isLoading) {
    return (
      <div className="min-h-[100dvh] bg-background">
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
          <Skeleton className="h-32 w-full rounded-lg" />
          {/* Property cards grid skeleton */}
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="p-4 rounded-lg border border-border-default space-y-3"
              >
                <Skeleton className="h-40 w-full rounded" />
                <Skeleton className="h-5 w-3/4" />
                <Skeleton className="h-4 w-1/2" />
                <div className="flex gap-2">
                  <Skeleton className="h-6 w-16 rounded-full" />
                  <Skeleton className="h-6 w-20 rounded-full" />
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
      <div className="min-h-[100dvh] bg-background p-8">
        <ErrorMessage message={t('list.error')} />
      </div>
    );
  }

  const headerActions: ListPageHeaderAction[] = [
    {
      label: t('common:refresh', 'Refresh'),
      render: () => (
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      ),
      // also rebound for overflow menu
      icon: RefreshCw,
      onClick: () => refetch(),
    },
    {
      label: 'Export',
      render: () => (
        <EntityExportControls
          filenameStem="properties"
          csv={exportPropertiesCsv}
          xlsx={exportPropertiesXlsx}
          googleSheet={exportPropertiesGoogleSheet}
        />
      ),
    },
  ];

  return (
    <div className="min-h-[100dvh] bg-background">
      <div className="px-4 py-4 md:py-8">
        <ListPageHeader
          title={t('list.title')}
          subtitle={t('list.subtitle')}
          icon={Home}
          mobileLeading={<MobileMenuButton />}
          actions={headerActions}
          primaryAction={{
            label: t('list.addButton'),
            icon: Plus,
            onClick: () => navigate('/properties/new'),
            disabled: !canEditData,
          }}
        />

        {/* Phone: search stays inline, filters move into FilterSheet trigger.
            md+: FilterSheet renders inline — same filter card UX as before. */}
        <div className="md:hidden mb-4 flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                resetPage();
              }}
              placeholder={t('list.searchPlaceholder')}
              className="w-full pl-10 pr-10 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
            {searchQuery && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  resetPage();
                }}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>
          <FilterSheet
            activeCount={(categoryFilter ? 1 : 0) + (statusFilter ? 1 : 0)}
            onClear={() => {
              setCategoryFilter(undefined);
              setStatusFilter(undefined);
              resetPage();
            }}
            triggerLabel={t('list.filters')}
            collapseBelow="lg"
          >
            <div className="space-y-4">
              {/* Category Filter */}
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('list.category')}
                </label>
                <div className="flex gap-2 flex-wrap">
                  {categoryFilters.map((filter) => (
                    <button
                      key={filter.label}
                      onClick={() => {
                        setCategoryFilter(filter.value);
                        resetPage();
                      }}
                      className={`px-4 py-2 rounded transition-colors text-sm min-h-touch ${
                        categoryFilter === filter.value
                          ? 'bg-primary-500 text-white'
                          : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
                      }`}
                    >
                      {filter.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Status Filter */}
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
            </div>
          </FilterSheet>
        </div>

        {/* Filter Bar (md+ only — preserves the desktop card UX) */}
        <div className="hidden md:block mb-6 bg-surface-card rounded-lg border border-border-default p-4 space-y-4">
          <div className="flex items-center gap-2">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h2 className="font-semibold text-text-primary">
              {t('list.filters')}
            </h2>
          </div>

          <div className="space-y-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  resetPage();
                }}
                placeholder={t('list.searchPlaceholder')}
                className="w-full pl-10 pr-10 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              />
              {searchQuery && (
                <button
                  onClick={() => {
                    setSearchQuery('');
                    resetPage();
                  }}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {/* Category Filter */}
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  {t('list.category')}
                </label>
                <div className="flex gap-2 flex-wrap">
                  {categoryFilters.map((filter) => (
                    <button
                      key={filter.label}
                      onClick={() => {
                        setCategoryFilter(filter.value);
                        resetPage();
                      }}
                      className={`px-4 py-2 rounded transition-colors text-sm ${
                        categoryFilter === filter.value
                          ? 'bg-primary-500 text-white'
                          : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
                      }`}
                    >
                      {filter.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Status Filter */}
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
                      className={`px-4 py-2 rounded transition-colors text-sm ${
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
            </div>
          </div>
        </div>

        {/* Property Count */}
        <p className="text-sm text-text-secondary mb-4">
          {t('list.count', { count: propertiesData?.totalElements ?? 0 })}
        </p>

        {/* Properties Grid */}
        {properties.length > 0 ? (
          <>
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {properties.map((property) => (
                <PropertyCard key={property.identifier} property={property} />
              ))}
            </div>
            {propertiesData && (
              <div className="mt-6">
                <Pagination
                  page={page}
                  totalPages={propertiesData.totalPages}
                  totalElements={propertiesData.totalElements}
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
              icon={<Home className="h-12 w-12" />}
              title={t('list.empty.title')}
              description={
                searchQuery || statusFilter || categoryFilter
                  ? t('list.empty.filtered')
                  : t('list.empty.noData')
              }
              actions={
                !searchQuery && !statusFilter && !categoryFilter ? (
                  <button
                    onClick={() => navigate('/properties/new')}
                    disabled={!canEditData}
                    className="bg-primary-500 text-white px-6 py-2 rounded min-h-touch hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500 focus-ring"
                  >
                    <Plus className="h-5 w-5" />
                    {t('list.addButton')}
                  </button>
                ) : undefined
              }
            />
          </div>
        )}
      </div>
    </div>
  );
};
