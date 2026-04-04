import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  PropertyStatus,
  PropertyCategory,
  PROPERTY_STATUS_LABELS,
  PROPERTY_CATEGORY_LABELS,
} from '@/types/property';
import { useProperties } from '@/hooks/usePropertyHooks';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home, Filter, Search, X } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { useDebounce } from '@/hooks/useDebounce';
import { LoadingSpinner, Pagination, RefreshButton } from '@buurman/ui';

const categoryFilters: {
  value: PropertyCategory | undefined;
  label: string;
}[] = [
  { value: undefined, label: 'All Categories' },
  ...Object.values(PropertyCategory).map((cat) => ({
    value: cat,
    label: PROPERTY_CATEGORY_LABELS[cat],
  })),
];

const statusFilters: { value: PropertyStatus | undefined; label: string }[] = [
  { value: undefined, label: 'All Statuses' },
  ...Object.values(PropertyStatus).map((status) => ({
    value: status,
    label: PROPERTY_STATUS_LABELS[status],
  })),
];

export const PropertyListPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
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
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load properties" />
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
                Properties
              </h1>
            </div>
            <p className="text-text-secondary ml-11">
              Manage your rental properties and units
            </p>
          </div>
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <button
              onClick={() => navigate('/properties/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              Add Property
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-surface-card rounded-lg border border-border-default p-4 space-y-4">
          <div className="flex items-center gap-2">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h2 className="font-semibold text-text-primary">Filters</h2>
          </div>

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
              placeholder="Search by address, city, postal code, or type..."
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
                Category
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
                        : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
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
        </div>

        {/* Property Count */}
        <p className="text-sm text-text-secondary mb-4">
          {propertiesData?.totalElements ?? 0}{' '}
          {propertiesData?.totalElements === 1 ? 'property' : 'properties'}
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
              No properties found
            </h3>
            <p className="text-text-secondary mb-6">
              {searchQuery || statusFilter || categoryFilter
                ? 'Try adjusting your filters or search terms'
                : 'Get started by adding your first property'}
            </p>
            {!searchQuery && !statusFilter && !categoryFilter && (
              <button
                onClick={() => navigate('/properties/new')}
                disabled={!canEditData}
                className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
              >
                <Plus className="h-5 w-5" />
                Add Property
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
