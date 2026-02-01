import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PropertyStatus } from '@/types/property';
import { useProperties } from '@/hooks/usePropertyHooks';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Home } from 'lucide-react';

const statusFilters = [
  { value: undefined, label: 'All' },
  { value: PropertyStatus.VACANT, label: 'Vacant' },
  { value: PropertyStatus.OCCUPIED, label: 'Occupied' },
  { value: PropertyStatus.MAINTENANCE, label: 'Maintenance' },
  { value: PropertyStatus.UNAVAILABLE, label: 'Unavailable' },
];

export const PropertyListPage = () => {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<PropertyStatus | undefined>(undefined);
  const { data: properties, isLoading, error } = useProperties(statusFilter);

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
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Properties</h1>
          <button
            onClick={() => navigate('/properties/new')}
            className="bg-primary text-white px-4 py-2 rounded hover:bg-primary-700 transition-colors flex items-center gap-2"
          >
            <Plus className="h-5 w-5" />
            Add Property
          </button>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 flex gap-2 flex-wrap">
          {statusFilters.map((filter) => (
            <button
              key={filter.label}
              onClick={() => setStatusFilter(filter.value)}
              className={`px-4 py-2 rounded transition-colors ${
                statusFilter === filter.value
                  ? 'bg-primary text-white'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-background'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>

        {/* Property Count */}
        <p className="text-sm text-gray-600 mb-4">
          {properties?.length || 0} {properties?.length === 1 ? 'property' : 'properties'}
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
          <div className="flex flex-col items-center justify-center py-16 bg-white rounded-lg">
            <Home className="h-16 w-16 text-gray-300 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No properties yet</h3>
            <p className="text-gray-600 mb-6">Get started by adding your first property</p>
            <button
              onClick={() => navigate('/properties/new')}
              className="bg-primary text-white px-6 py-2 rounded hover:bg-primary-700 transition-colors flex items-center gap-2"
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
