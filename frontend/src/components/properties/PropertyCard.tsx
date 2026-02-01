import { PropertyResponse, PropertyStatus } from '@/types/property';
import { Bed, Bath, Ruler, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface PropertyCardProps {
  property: PropertyResponse;
}

const statusColors: Record<PropertyStatus, string> = {
  VACANT: 'bg-green-100 text-green-800',
  OCCUPIED: 'bg-blue-100 text-blue-800',
  MAINTENANCE: 'bg-yellow-100 text-yellow-800',
  UNAVAILABLE: 'bg-gray-100 text-gray-800',
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
      className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/properties/${property.id}`)}
    >
      {/* Property Image */}
      <div className="relative bg-gray-200 h-48 flex items-center justify-center overflow-hidden">
        {property.mainPhotoUrl ? (
          <img
            src={property.mainPhotoUrl}
            alt={property.street}
            className="w-full h-full object-cover"
          />
        ) : (
          <Home className="h-16 w-16 text-gray-400" />
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
        <h3 className="text-lg font-semibold text-gray-900 mb-1">
          {property.street}
        </h3>
        <p className="text-sm text-gray-600 mb-3">
          {property.city}, {property.postalCode}
        </p>

        {/* Specifications */}
        <div className="grid grid-cols-3 gap-2 mb-3">
          {property.bedrooms !== null && (
            <div className="flex items-center gap-1 text-gray-700">
              <Bed className="h-4 w-4 text-gray-400" />
              <span className="text-sm">{property.bedrooms}</span>
            </div>
          )}
          {property.bathrooms !== null && (
            <div className="flex items-center gap-1 text-gray-700">
              <Bath className="h-4 w-4 text-gray-400" />
              <span className="text-sm">{property.bathrooms}</span>
            </div>
          )}
          {property.squareMeters !== null && (
            <div className="flex items-center gap-1 text-gray-700">
              <Ruler className="h-4 w-4 text-gray-400" />
              <span className="text-sm">{property.squareMeters}m²</span>
            </div>
          )}
        </div>

        {/* Property Type */}
        <div className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded inline-block">
          {property.propertyType.replace('_', ' ')}
        </div>
      </div>
    </div>
  );
};
