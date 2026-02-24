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
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home, Filter, Search, X } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { Pagination } from '@/components/ui/Pagination';
import { RefreshButton } from '@/components/ui/RefreshButton';

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
  const [debouncedQuery, setDebouncedQuery] = useState('');

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    resetPage,
  } = usePagination({ defaultSize: 12 });

  // Debounce search input
  const [debounceTimer, setDebounceTimer] =
    useState<ReturnType<typeof setTimeout>>();
  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    if (debounceTimer) {
      clearTimeout(debounceTimer);
    }
    const timer = setTimeout(() => {
      setDebouncedQuery(value);
      resetPage();
    }, 400);
    setDebounceTimer(timer);
  };

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
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Properties
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
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
              className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
            >
              <Plus className="h-5 w-5" />
              Add Property
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4 space-y-4">
          <div className="flex items-center gap-2">
            <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
            <h2 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Filters
            </h2>
          </div>

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8]" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search by address, city, postal code, or type..."
              className="w-full pl-10 pr-10 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            />
            {searchQuery && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  setDebouncedQuery('');
                  resetPage();
                }}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9ca0b8] hover:text-[#3d4463]"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Category Filter */}
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
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
                        ? 'bg-[#5c7cfa] text-white'
                        : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Status Filter */}
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
                        : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
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
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
          {propertiesData?.totalElements || 0}{' '}
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
          <div className="flex flex-col items-center justify-center py-16 bg-white dark:bg-[#14161f] rounded-lg">
            <Home className="h-16 w-16 text-[#c9cfd9] dark:text-[#3a3f54] mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No properties found
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
              {searchQuery || statusFilter || categoryFilter
                ? 'Try adjusting your filters or search terms'
                : 'Get started by adding your first property'}
            </p>
            {!searchQuery && !statusFilter && !categoryFilter && (
              <button
                onClick={() => navigate('/properties/new')}
                disabled={!canEditData}
                className="bg-[#5c7cfa] text-white px-6 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
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
