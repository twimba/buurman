import { useNavigate } from 'react-router-dom';
import {
  PropertyStatus,
  PropertyType,
  PropertyCategory,
  PROPERTY_STATUS_LABELS,
} from '@/types/property';
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
  [PropertyStatus.VACANT]: 'bg-emerald-500',
  [PropertyStatus.OCCUPIED]: 'bg-blue-500',
  [PropertyStatus.MAINTENANCE]: 'bg-amber-500',
  [PropertyStatus.UNAVAILABLE]: 'bg-[#9ca0b8] dark:bg-[#5c6180]',
  [PropertyStatus.UNDER_RENOVATION]: 'bg-orange-500',
  [PropertyStatus.FALLOW]: 'bg-stone-500',
  [PropertyStatus.LISTED]: 'bg-purple-500',
};

const iconCls = 'h-3.5 w-3.5 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0';

const CategoryIcon = ({
  category,
  type,
}: {
  category?: PropertyCategory;
  type?: PropertyType;
}) => {
  if (category === PropertyCategory.INDUSTRIAL)
    return <Factory className={iconCls} />;
  if (category === PropertyCategory.AGRICULTURAL)
    return <Tractor className={iconCls} />;
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
        hover:bg-blue-50 dark:hover:bg-[#1e2130]
        transition-colors
        focus:outline-none
        focus:ring-2
        focus:ring-blue-500/20
      "
    >
      <div className="flex items-start gap-3">
        {/* Status indicator */}
        <div className="flex-shrink-0 pt-1">
          <div
            className={`w-2 h-2 rounded-full ${statusColors[propertyStatus] ?? 'bg-gray-400'}`}
            title={PROPERTY_STATUS_LABELS[propertyStatus] ?? propertyStatus}
          />
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          {/* Street */}
          <div className="flex items-center gap-1.5 text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] group-hover:text-[#5c7cfa] dark:group-hover:text-blue-400 transition-colors">
            <CategoryIcon category={propertyCategory} type={propertyType} />
            <span className="truncate">{street}</span>
          </div>

          {/* City & Postal */}
          <div className="flex items-center gap-1 mt-0.5 text-xs text-[#6b7194] dark:text-[#8b90a8]">
            <MapPin className="h-3 w-3 flex-shrink-0" />
            <span className="truncate">
              {city}, {postalCode}
            </span>
          </div>

          {/* Property ID */}
          <div className="text-[10px] text-[#9ca0b8] dark:text-[#5c6180] mt-0.5 font-mono">
            #{propertyIdentifier}
          </div>
        </div>
      </div>
    </button>
  );
};
