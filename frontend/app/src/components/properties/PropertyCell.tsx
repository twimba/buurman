import { useNavigate } from 'react-router-dom';
import {
  PropertyStatus,
  PropertyType,
  PropertyCategory,
} from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { Home, Building2, Factory, Tractor, MapPin } from 'lucide-react';

interface PropertyCellProps {
  propertyIdentifier: string;
  propertyStatus: PropertyStatus;
  propertyType: PropertyType;
  propertyCategory?: PropertyCategory;
  street: string;
  city: string;
  postalCode: string;
  onClick?: (e: React.MouseEvent) => void;
}

const statusColors: Record<string, string> = {
  [PropertyStatus.VACANT]: 'bg-success-text',
  [PropertyStatus.OCCUPIED]: 'bg-info-text',
  [PropertyStatus.MAINTENANCE]: 'bg-warning-text',
  [PropertyStatus.UNAVAILABLE]: 'bg-neutral-400',
  [PropertyStatus.UNDER_RENOVATION]: 'bg-warning-text',
  [PropertyStatus.FALLOW]: 'bg-neutral-500',
  [PropertyStatus.LISTED]: 'bg-info-text',
};

const iconCls = 'h-3.5 w-3.5 text-text-muted flex-shrink-0';

const CategoryIcon = ({
  category,
  type,
}: {
  category?: PropertyCategory;
  type?: PropertyType;
}) => {
  if (category === PropertyCategory.INDUSTRIAL) {
    return <Factory className={iconCls} />;
  }
  if (category === PropertyCategory.AGRICULTURAL) {
    return <Tractor className={iconCls} />;
  }
  if (
    category === PropertyCategory.COMMERCIAL ||
    type === PropertyType.COMMERCIAL
  )
    return <Building2 className={iconCls} />;
  return <Home className={iconCls} />;
};

export const PropertyCell = ({
  propertyIdentifier,
  propertyStatus,
  propertyType,
  propertyCategory,
  street,
  city,
  postalCode,
  onClick,
}: PropertyCellProps) => {
  const navigate = useNavigate();
  const { statusLabel } = usePropertyLabels();

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onClick) {
      onClick(e);
    } else {
      navigate(`/properties/${propertyIdentifier}`);
    }
  };

  return (
    <button
      onClick={handleClick}
      className="
        group
        text-left
        w-full
        p-2
        -m-2
        rounded-lg
        hover:bg-primary-50
        transition-colors
        focus:outline-none
        focus:ring-2
        focus:ring-primary-500/20
"
    >
      <div className="flex items-start gap-3">
        {/* Status indicator */}
        <div className="flex-shrink-0 pt-1">
          <div
            className={`w-2 h-2 rounded-full ${statusColors[propertyStatus] ?? 'bg-gray-400'}`}
            title={statusLabel(propertyStatus)}
          />
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          {/* Street */}
          <div className="flex items-center gap-1.5 text-sm font-medium text-text-primary group-hover:text-primary-500 transition-colors">
            <CategoryIcon category={propertyCategory} type={propertyType} />
            <span className="truncate">{street}</span>
          </div>

          {/* City & Postal */}
          <div className="flex items-center gap-1 mt-0.5 text-xs text-text-secondary">
            <MapPin className="h-3 w-3 flex-shrink-0" />
            <span className="truncate">
              {city}, {postalCode}
            </span>
          </div>

          {/* Property ID */}
          <div className="text-[10px] text-text-muted mt-0.5 font-mono">
            #{propertyIdentifier}
          </div>
        </div>
      </div>
    </button>
  );
};
