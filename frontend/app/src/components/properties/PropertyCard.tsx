import { useTranslation } from 'react-i18next';
import { PropertyResponse } from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { useNavigate } from 'react-router-dom';
import { PropertyTypeIcon } from '@/components/common/PropertyTypeIcon';
import { LazyImage } from '@buurman/ui';
import { describePropertyOccupancy } from '@/utils/propertyOccupancy';
import {
  PROPERTY_CATEGORY_ICONS,
  PROPERTY_TYPE_ICONS,
} from '@/utils/propertyIcons';

interface PropertyCardProps {
  property: PropertyResponse;
}

export const PropertyCard = ({ property }: PropertyCardProps) => {
  const navigate = useNavigate();
  const { t } = useTranslation('properties');
  const { typeLabel, categoryLabel } = usePropertyLabels();
  const PlaceholderIcon =
    PROPERTY_TYPE_ICONS[property.propertyType] ??
    PROPERTY_CATEGORY_ICONS[property.propertyCategory] ??
    PROPERTY_CATEGORY_ICONS.RESIDENTIAL;
  // A property can hold several independently-let units (BUUR-106), so there is no single
  // property-level status anymore -- derive a coarse one from unit counts instead.
  const occupancy = describePropertyOccupancy(
    t,
    property.unitCount,
    property.occupiedUnitCount
  );

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/properties/${property.identifier}`)}
    >
      {/* Property Image */}
      <div className="relative bg-surface-inset h-48 flex items-center justify-center overflow-hidden">
        {property.mainPhotoUrl ? (
          <LazyImage
            src={property.mainPhotoThumbnailUrl ?? property.mainPhotoUrl}
            alt={property.street}
            className="w-full h-full object-cover"
          />
        ) : (
          <PlaceholderIcon className="h-16 w-16 text-text-muted " />
        )}
        {/* Occupancy Badge */}
        <div className="absolute top-3 right-3">
          <span
            className={`px-3 py-1 rounded-full text-xs font-semibold ${occupancy.colorClass}`}
          >
            {occupancy.label}
          </span>
        </div>
      </div>

      {/* Property Details */}
      <div className="p-4">
        {/* Address */}
        <h3 className="text-lg font-semibold text-text-primary mb-1">
          {property.street}
        </h3>
        <p className="hidden md:block text-sm text-text-secondary mb-1">
          #{property.identifier}
        </p>
        <p className="text-sm text-text-secondary mb-3">
          {property.city}, {property.postalCode}
        </p>

        {/* Category & Type badges */}
        <div className="flex gap-1.5 flex-wrap">
          <span className="inline-flex items-center gap-1 text-xs bg-surface-inset text-text-secondary px-2 py-1 rounded">
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
