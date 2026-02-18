import {
  PropertyResponse,
  PropertyStatus,
  PropertyCategory,
  PROPERTY_TYPE_LABELS,
  PROPERTY_CATEGORY_LABELS,
  PROPERTY_STATUS_LABELS,
} from '@/types/property';
import {
  Bed,
  Bath,
  Ruler,
  Home,
  Building2,
  Factory,
  Tractor,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface PropertyCardProps {
  property: PropertyResponse;
}

const statusColors: Record<string, string> = {
  VACANT: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  OCCUPIED: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
  MAINTENANCE:
    'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  UNAVAILABLE:
    'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db]',
  UNDER_RENOVATION:
    'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200',
  FALLOW: 'bg-stone-100 text-stone-800 dark:bg-stone-900 dark:text-stone-200',
  LISTED:
    'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
};

const categoryIcons: Record<string, typeof Home> = {
  [PropertyCategory.RESIDENTIAL]: Home,
  [PropertyCategory.COMMERCIAL]: Building2,
  [PropertyCategory.INDUSTRIAL]: Factory,
  [PropertyCategory.AGRICULTURAL]: Tractor,
  [PropertyCategory.MIXED_USE]: Building2,
};

export const PropertyCard = ({ property }: PropertyCardProps) => {
  const navigate = useNavigate();
  const PlaceholderIcon = categoryIcons[property.propertyCategory] ?? Home;
  const bedrooms = property.residentialDetails?.bedrooms;
  const bathrooms = property.residentialDetails?.bathrooms;

  return (
    <div
      className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/properties/${property.identifier}`)}
    >
      {/* Property Image */}
      <div className="relative bg-[#e8ecf4] dark:bg-[#1e2130] h-48 flex items-center justify-center overflow-hidden">
        {property.mainPhotoUrl ? (
          <img
            src={property.mainPhotoThumbnailUrl ?? property.mainPhotoUrl}
            alt={property.street}
            className="w-full h-full object-cover"
            loading="lazy"
          />
        ) : (
          <PlaceholderIcon className="h-16 w-16 text-[#9ca0b8] dark:text-[#5c6180]" />
        )}
        {/* Status Badge */}
        <div className="absolute top-3 right-3">
          <span
            className={`px-3 py-1 rounded-full text-xs font-semibold ${statusColors[property.status] ?? 'bg-gray-100 text-gray-800'}`}
          >
            {PROPERTY_STATUS_LABELS[property.status as PropertyStatus] ??
              property.status}
          </span>
        </div>
      </div>

      {/* Property Details */}
      <div className="p-4">
        {/* Address */}
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-1">
          {property.street}
        </h3>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-1">
          #{property.identifier}
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-3">
          {property.city}, {property.postalCode}
        </p>

        {/* Specifications */}
        <div className="grid grid-cols-3 gap-2 mb-3">
          {bedrooms != null && (
            <div className="flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]">
              <Bed className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
              <span className="text-sm">{bedrooms}</span>
            </div>
          )}
          {bathrooms != null && (
            <div className="flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]">
              <Bath className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
              <span className="text-sm">{bathrooms}</span>
            </div>
          )}
          {property.areaValue !== null && (
            <div className="flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]">
              <Ruler className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
              <span className="text-sm">
                {property.areaValue}
                {property.areaUnit === 'sqft' ? 'ft²' : 'm²'}
              </span>
            </div>
          )}
        </div>

        {/* Category & Type badges */}
        <div className="flex gap-1.5 flex-wrap">
          <span className="text-xs bg-[#e8ecf4] dark:bg-[#1a1d28] text-[#6b7194] dark:text-[#8b90a8] px-2 py-1 rounded">
            {PROPERTY_CATEGORY_LABELS[property.propertyCategory] ??
              property.propertyCategory}
          </span>
          <span className="text-xs bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] px-2 py-1 rounded">
            {PROPERTY_TYPE_LABELS[property.propertyType] ??
              property.propertyType}
          </span>
        </div>
      </div>
    </div>
  );
};
