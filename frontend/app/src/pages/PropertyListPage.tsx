import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PropertyStatus, PropertyCategory } from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { useProperties } from '@/hooks/usePropertyHooks';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home, Search, X, Layers, CircleDot } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { useDebounce } from '@/hooks/useDebounce';
import {
  EmptyState,
  FilterSelectPopover,
  ListPageHeader,
  Pagination,
  RefreshButton,
  Skeleton,
  type ListPageHeaderAction,
} from '@buurman/ui';
import { EntityExportControls } from '@/components/common/EntityExportControls';
import {
  exportPropertiesCsv,
  exportPropertiesXlsx,
  exportPropertiesGoogleSheet,
} from '@/generated/api/booklets/booklets';
import { GOOGLE_SHEET_EXPORT_TIMEOUT_MS } from '@/utils/googleSheetExport';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { RefreshCw } from 'lucide-react';

export const PropertyListPage = () => {
  const { t } = useTranslation('properties');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { statusLabel, categoryLabel } = usePropertyLabels();

  const categoryOptions = useMemo(
    () =>
      Object.values(PropertyCategory).map((cat) => ({
        value: cat,
        label: categoryLabel(cat),
      })),
    [categoryLabel]
  );

  const statusOptions = useMemo(
    () =>
      Object.values(PropertyStatus).map((status) => ({
        value: status,
        label: statusLabel(status),
      })),
    [statusLabel]
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
      <div className="min-h-full bg-background p-8">
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
          csv={() => exportPropertiesCsv()}
          xlsx={() => exportPropertiesXlsx()}
          googleSheet={(accessToken) =>
            exportPropertiesGoogleSheet(
              { accessToken },
              { timeout: GOOGLE_SHEET_EXPORT_TIMEOUT_MS }
            )
          }
        />
      ),
    },
  ];

  return (
    <div className="min-h-full bg-background">
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

        {/* Toolbar — search + filter dropdowns on one tidy row */}
        <div className="flex flex-wrap items-center gap-2 mb-4">
          <div className="relative flex-1 min-w-[220px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                resetPage();
              }}
              placeholder={t('list.searchPlaceholder')}
              aria-label={t('list.searchPlaceholder')}
              className="w-full h-10 pl-10 pr-9 border border-border-strong rounded-lg focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
            />
            {searchQuery && (
              <button
                onClick={() => {
                  setSearchQuery('');
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
            icon={Layers}
            label={t('list.category')}
            options={categoryOptions}
            value={categoryFilter}
            onChange={(value) => {
              setCategoryFilter(value);
              resetPage();
            }}
            allLabel={t('list.allCategories')}
          />
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
        </div>

        {/* Active filter chips */}
        {(categoryFilter || statusFilter) && (
          <div className="flex flex-wrap items-center gap-2 mb-4">
            {categoryFilter && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300">
                {categoryLabel(categoryFilter)}
                <button
                  onClick={() => {
                    setCategoryFilter(undefined);
                    resetPage();
                  }}
                  aria-label={t('common:buttons.clear', 'Clear')}
                  className="rounded-full hover:text-primary-900 focus-ring"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
            {statusFilter && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-surface-inset text-text-secondary">
                {statusLabel(statusFilter)}
                <button
                  onClick={() => {
                    setStatusFilter(undefined);
                    resetPage();
                  }}
                  aria-label={t('common:buttons.clear', 'Clear')}
                  className="rounded-full hover:text-text-primary focus-ring"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
            <button
              onClick={() => {
                setCategoryFilter(undefined);
                setStatusFilter(undefined);
                resetPage();
              }}
              className="text-xs text-primary-500 hover:text-primary-600 rounded focus-ring"
            >
              {t('list.clearAll', 'Clear all')}
            </button>
          </div>
        )}

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
                  totalPages={propertiesData.totalPages ?? 0}
                  totalElements={propertiesData.totalElements ?? 0}
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
