import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { PropertyStatus, PropertyType } from '@/types/property';
import { useProperties } from '@/hooks/usePropertyHooks';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home, Filter } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';

const statusFilters = [
  { value: undefined, label: 'All Statuses' },
  { value: PropertyStatus.VACANT, label: 'Vacant' },
  { value: PropertyStatus.OCCUPIED, label: 'Occupied' },
  { value: PropertyStatus.MAINTENANCE, label: 'Maintenance' },
  { value: PropertyStatus.UNAVAILABLE, label: 'Unavailable' },
];

const typeFilters = [
  { value: undefined, label: 'All Types' },
  { value: PropertyType.APARTMENT, label: 'Apartment' },
  { value: PropertyType.HOUSE, label: 'House' },
  { value: PropertyType.STUDIO, label: 'Studio' },
  { value: PropertyType.COMMERCIAL, label: 'Commercial' },
];

export const PropertyListPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [statusFilter, setStatusFilter] = useState<PropertyStatus | undefined>(
    undefined
  );
  const [typeFilter, setTypeFilter] = useState<PropertyType | undefined>(
    undefined
  );
  const { data: allProperties, isLoading, error } = useProperties(statusFilter);

  // Client-side filtering by property type
  const properties = useMemo(() => {
    if (!allProperties) return [];
    if (!typeFilter) return allProperties;
    return allProperties.filter(
      (property) => property.propertyType === typeFilter
    );
  }, [allProperties, typeFilter]);

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
              <Home className="h-8 w-8 text-blue-600 dark:text-blue-400" />
              <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
                Properties
              </h1>
            </div>
            <p className="text-gray-600 dark:text-gray-400 ml-11">
              Manage your rental properties and units
            </p>
          </div>
          <button
            onClick={() => navigate('/properties/new')}
            disabled={!canEditData}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-blue-600"
          >
            <Plus className="h-5 w-5" />
            Add Property
          </button>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            <h2 className="font-semibold text-gray-900 dark:text-gray-100">
              Filters
            </h2>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Property Type Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Property Type
              </label>
              <div className="flex gap-2 flex-wrap">
                {typeFilters.map((filter) => (
                  <button
                    key={filter.label}
                    onClick={() => setTypeFilter(filter.value)}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      typeFilter === filter.value
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Status Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
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
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
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
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
          {properties?.length || 0}{' '}
          {properties?.length === 1 ? 'property' : 'properties'}
        </p>

        {/* Properties Grid */}
        {properties && properties.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {properties.map((property) => (
              <PropertyCard key={property.id} property={property} />
            ))}
          </div>
        ) : (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-white dark:bg-gray-800 rounded-lg">
            <Home className="h-16 w-16 text-gray-300 dark:text-gray-600 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
              No properties yet
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Get started by adding your first property
            </p>
            <button
              onClick={() => navigate('/properties/new')}
              disabled={!canEditData}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-blue-600"
            >
              <Plus className="h-5 w-5" />
              Add Property
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
