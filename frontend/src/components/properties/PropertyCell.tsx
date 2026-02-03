import { useNavigate } from 'react-router-dom';
import { PropertyStatus, PropertyType } from '@/types/property';
import { Home, Building2, MapPin } from 'lucide-react';

interface PropertyCellProps {
  propertyId: string;
  propertyIdentifier: string;
  propertyStatus: PropertyStatus;
  propertyType: PropertyType;
  street: string;
  city: string;
  postalCode: string;
  onClick?: (e: React.MouseEvent) => void;
}

const statusColors: Record<PropertyStatus, string> = {
  [PropertyStatus.VACANT]: 'bg-emerald-500',
  [PropertyStatus.OCCUPIED]: 'bg-blue-500',
  [PropertyStatus.MAINTENANCE]: 'bg-amber-500',
  [PropertyStatus.UNAVAILABLE]: 'bg-gray-400',
};

const statusLabels: Record<PropertyStatus, string> = {
  [PropertyStatus.VACANT]: 'Vacant',
  [PropertyStatus.OCCUPIED]: 'Occupied',
  [PropertyStatus.MAINTENANCE]: 'Maintenance',
  [PropertyStatus.UNAVAILABLE]: 'Unavailable',
};

export const PropertyCell = ({
  propertyId,
  propertyIdentifier,
  propertyStatus,
  propertyType,
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
      navigate(`/properties/${propertyId}`);
    }
  };

  const PropertyIcon =
    propertyType === PropertyType.COMMERCIAL ? Building2 : Home;

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
        hover:bg-blue-50
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
            className={`w-2 h-2 rounded-full ${statusColors[propertyStatus]}`}
            title={statusLabels[propertyStatus]}
          />
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          {/* Street */}
          <div className="flex items-center gap-1.5 text-sm font-medium text-gray-900 group-hover:text-blue-600 transition-colors">
            <PropertyIcon className="h-3.5 w-3.5 text-gray-400 flex-shrink-0" />
            <span className="truncate">{street}</span>
          </div>

          {/* City & Postal */}
          <div className="flex items-center gap-1 mt-0.5 text-xs text-gray-500">
            <MapPin className="h-3 w-3 flex-shrink-0" />
            <span className="truncate">
              {city}, {postalCode}
            </span>
          </div>

          {/* Property ID */}
          <div className="text-[10px] text-gray-400 mt-0.5 font-mono">
            #{propertyIdentifier}
          </div>
        </div>
      </div>
    </button>
  );
};
