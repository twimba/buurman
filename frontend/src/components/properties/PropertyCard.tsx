import { PropertyResponse, PropertyStatus } from '@/types/property';
import { Bed, Bath, Ruler, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface PropertyCardProps {
  property: PropertyResponse;
}

const statusColors: Record<PropertyStatus, string> = {
  VACANT: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  OCCUPIED: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  MAINTENANCE:
    'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  UNAVAILABLE: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200',
};

const statusLabels: Record<PropertyStatus, string> = {
  VACANT: 'Vacant',
  OCCUPIED: 'Occupied',
  MAINTENANCE: 'Maintenance',
  UNAVAILABLE: 'Unavailable',
};

export const PropertyCard = ({ property }: PropertyCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-white dark:bg-gray-800 rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/properties/${property.id}`)}
    >
      {/* Property Image */}
      <div className="relative bg-gray-200 dark:bg-gray-700 h-48 flex items-center justify-center overflow-hidden">
        {property.mainPhotoUrl ? (
          <img
            src={property.mainPhotoUrl}
            alt={property.street}
            className="w-full h-full object-cover"
          />
        ) : (
          <Home className="h-16 w-16 text-gray-400 dark:text-gray-500" />
        )}
        {/* Status Badge */}
        <div className="absolute top-3 right-3">
          <span
            className={`px-3 py-1 rounded-full text-xs font-semibold ${statusColors[property.status]}`}
          >
            {statusLabels[property.status]}
          </span>
        </div>
      </div>

      {/* Property Details */}
      <div className="p-4">
        {/* Address */}
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
          {property.street}
        </h3>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">
          #{property.identifier}
        </p>
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
          {property.city}, {property.postalCode}
        </p>

        {/* Specifications */}
        <div className="grid grid-cols-3 gap-2 mb-3">
          {property.bedrooms !== null && (
            <div className="flex items-center gap-1 text-gray-700 dark:text-gray-300">
              <Bed className="h-4 w-4 text-gray-400 dark:text-gray-500" />
              <span className="text-sm">{property.bedrooms}</span>
            </div>
          )}
          {property.bathrooms !== null && (
            <div className="flex items-center gap-1 text-gray-700 dark:text-gray-300">
              <Bath className="h-4 w-4 text-gray-400 dark:text-gray-500" />
              <span className="text-sm">{property.bathrooms}</span>
            </div>
          )}
          {property.squareMeters !== null && (
            <div className="flex items-center gap-1 text-gray-700 dark:text-gray-300">
              <Ruler className="h-4 w-4 text-gray-400 dark:text-gray-500" />
              <span className="text-sm">{property.squareMeters}m²</span>
            </div>
          )}
        </div>

        {/* Property Type */}
        <div className="text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 rounded inline-block">
          {property.propertyType.replace('_', ' ')}
        </div>
      </div>
    </div>
  );
};
