import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PropertyStatus, PropertyCategory } from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { useProperties } from '@/hooks/usePropertyHooks';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home, Filter, Search, X, ChevronDown } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { useDebounce } from '@/hooks/useDebounce';
import { Pagination, RefreshButton, Skeleton } from '@buurman/ui';
import { EntityExportControls } from '@/components/common/EntityExportControls';
import { exportPropertiesCsv, exportPropertiesXlsx } from '@/api/listExports';
import { exportPropertiesGoogleSheet } from '@/api/googleSheetsExport';

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
  // Phase 0: filter card collapsed by default on phone (md hidden bypasses this state).
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
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
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message={t('list.error')} />
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
              <Home className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">
                {t('list.title')}
              </h1>
            </div>
            <p className="text-text-secondary ml-11">{t('list.subtitle')}</p>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <EntityExportControls
              filenameStem="properties"
              csv={exportPropertiesCsv}
              xlsx={exportPropertiesXlsx}
              googleSheet={exportPropertiesGoogleSheet}
            />
            <button
              onClick={() => navigate('/properties/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              {t('list.addButton')}
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-surface-card rounded-lg border border-border-default p-4 space-y-4">
          {/* Phone: collapsible toggle. md+: always visible header — desktop unchanged. */}
          <button
            type="button"
            onClick={() => setMobileFiltersOpen((o) => !o)}
            aria-expanded={mobileFiltersOpen}
            aria-controls="property-filter-body"
            className="md:hidden w-full flex items-center justify-between min-h-11"
          >
            <span className="flex items-center gap-2">
              <Filter className="h-5 w-5 text-text-secondary " />
              <span className="font-semibold text-text-primary">
                {t('list.filters')}
              </span>
            </span>
            <ChevronDown
              className={`h-5 w-5 text-text-secondary transition-transform ${
                mobileFiltersOpen ? 'rotate-180' : ''
              }`}
            />
          </button>
          <div className="hidden md:flex items-center gap-2">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h2 className="font-semibold text-text-primary">
              {t('list.filters')}
            </h2>
          </div>

          {/* Body: collapsed on phone when mobileFiltersOpen=false, always visible md+ */}
          <div
            id="property-filter-body"
            className={`space-y-4 ${mobileFiltersOpen ? 'block' : 'hidden'} md:block`}
          >
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
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-surface-card rounded-lg">
            <Home className="h-16 w-16 text-text-disabled mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              {t('list.empty.title')}
            </h3>
            <p className="text-text-secondary mb-6">
              {searchQuery || statusFilter || categoryFilter
                ? t('list.empty.filtered')
                : t('list.empty.noData')}
            </p>
            {!searchQuery && !statusFilter && !categoryFilter && (
              <button
                onClick={() => navigate('/properties/new')}
                disabled={!canEditData}
                className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
              >
                <Plus className="h-5 w-5" />
                {t('list.addButton')}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
