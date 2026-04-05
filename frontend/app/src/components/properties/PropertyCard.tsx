import { PropertyResponse } from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { Bed, Bath, Ruler } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PropertyTypeIcon } from '@/components/common/PropertyTypeIcon';
import {
  PROPERTY_CATEGORY_ICONS,
  PROPERTY_TYPE_ICONS,
} from '@/utils/propertyIcons';

interface PropertyCardProps {
  property: PropertyResponse;
}

const statusColors: Record<string, string> = {
  VACANT: 'bg-success-bg text-success-text',
  OCCUPIED: 'bg-info-bg text-info-text',
  MAINTENANCE: 'bg-warning-bg text-warning-text',
  UNAVAILABLE: 'bg-surface-inset text-text-primary',
  UNDER_RENOVATION: 'bg-warning-bg text-warning-text',
  FALLOW: 'bg-surface-inset text-text-secondary',
  LISTED: 'bg-info-bg text-info-text',
  SELF_OCCUPIED: 'bg-info-bg text-info-text',
};

export const PropertyCard = ({ property }: PropertyCardProps) => {
  const navigate = useNavigate();
  const { statusLabel, typeLabel, categoryLabel } = usePropertyLabels();
  const PlaceholderIcon =
    PROPERTY_TYPE_ICONS[property.propertyType] ??
    PROPERTY_CATEGORY_ICONS[property.propertyCategory] ??
    PROPERTY_CATEGORY_ICONS.RESIDENTIAL;
  const bedrooms = property.residentialDetails?.bedrooms;
  const bathrooms = property.residentialDetails?.bathrooms;

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/properties/${property.identifier}`)}
    >
      {/* Property Image */}
      <div className="relative bg-neutral-100 h-48 flex items-center justify-center overflow-hidden">
        {property.mainPhotoUrl ? (
          <img
            src={property.mainPhotoThumbnailUrl ?? property.mainPhotoUrl}
            alt={property.street}
            className="w-full h-full object-cover"
            loading="lazy"
          />
        ) : (
          <PlaceholderIcon className="h-16 w-16 text-text-muted " />
        )}
        {/* Status Badge */}
        <div className="absolute top-3 right-3">
          <span
            className={`px-3 py-1 rounded-full text-xs font-semibold ${statusColors[property.status] ?? 'bg-gray-100 text-gray-800'}`}
          >
            {statusLabel(property.status)}
          </span>
        </div>
      </div>

      {/* Property Details */}
      <div className="p-4">
        {/* Address */}
        <h3 className="text-lg font-semibold text-text-primary mb-1">
          {property.street}
        </h3>
        <p className="text-sm text-text-secondary mb-1">
          #{property.identifier}
        </p>
        <p className="text-sm text-text-secondary mb-3">
          {property.city}, {property.postalCode}
        </p>

        {/* Specifications */}
        <div className="grid grid-cols-3 gap-2 mb-3">
          {bedrooms != null && (
            <div className="flex items-center gap-1 text-text-secondary">
              <Bed className="h-4 w-4 text-text-muted " />
              <span className="text-sm">{bedrooms}</span>
            </div>
          )}
          {bathrooms != null && (
            <div className="flex items-center gap-1 text-text-secondary">
              <Bath className="h-4 w-4 text-text-muted " />
              <span className="text-sm">{bathrooms}</span>
            </div>
          )}
          {property.areaValue != null && (
            <div className="flex items-center gap-1 text-text-secondary">
              <Ruler className="h-4 w-4 text-text-muted " />
              <span className="text-sm">
                {property.areaValue}
                {property.areaUnit === 'sqft' ? 'ft²' : 'm²'}
              </span>
            </div>
          )}
        </div>

        {/* Category & Type badges */}
        <div className="flex gap-1.5 flex-wrap">
          <span className="inline-flex items-center gap-1 text-xs bg-neutral-100 text-text-secondary px-2 py-1 rounded">
            <PropertyTypeIcon
              category={property.propertyCategory}
              size={11}
              className="flex-shrink-0"
            />
            {categoryLabel(property.propertyCategory)}
          </span>
          <span className="inline-flex items-center gap-1 text-xs bg-surface-inset text-text-secondary px-2 py-1 rounded">
            <PropertyTypeIcon
              type={property.propertyType}
              size={11}
              className="flex-shrink-0"
            />
            {typeLabel(property.propertyType)}
          </span>
        </div>
      </div>
    </div>
  );
};
