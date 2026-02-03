import { useNavigate } from 'react-router-dom';
import { ContractStatus } from '@/types/contract';
import { Home, User } from 'lucide-react';

interface ContractCellProps {
  contractId: string;
  contractIdentifier: string;
  contractStatus: ContractStatus;
  propertyStreet: string;
  propertyCity: string;
  tenantFirstName: string;
  tenantLastName?: string;
  onClick?: (e: React.MouseEvent) => void;
}

const statusColors: Record<ContractStatus, string> = {
  [ContractStatus.DRAFT]: 'bg-gray-400',
  [ContractStatus.PENDING_SIGNATURE]: 'bg-yellow-400',
  [ContractStatus.ACTIVE]: 'bg-emerald-500',
  [ContractStatus.EXPIRED]: 'bg-orange-400',
  [ContractStatus.TERMINATED]: 'bg-red-400',
};

const statusLabels: Record<ContractStatus, string> = {
  [ContractStatus.DRAFT]: 'Draft',
  [ContractStatus.PENDING_SIGNATURE]: 'Pending',
  [ContractStatus.ACTIVE]: 'Active',
  [ContractStatus.EXPIRED]: 'Expired',
  [ContractStatus.TERMINATED]: 'Terminated',
};

export const ContractCell = ({
  contractId,
  contractIdentifier,
  contractStatus,
  propertyStreet,
  propertyCity,
  tenantFirstName,
  tenantLastName,
  onClick,
}: ContractCellProps) => {
  const navigate = useNavigate();

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onClick) {
      onClick(e);
    } else {
      navigate(`/contracts/${contractId}`);
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
        hover:bg-blue-50 dark:hover:bg-gray-700
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
            className={`w-2 h-2 rounded-full ${statusColors[contractStatus]}`}
            title={statusLabels[contractStatus]}
          />
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          {/* Property */}
          <div className="flex items-center gap-1.5 text-sm font-medium text-gray-900 dark:text-gray-100 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
            <Home className="h-3.5 w-3.5 text-gray-400 dark:text-gray-500 flex-shrink-0" />
            <span className="truncate">{propertyStreet}</span>
          </div>

          {/* Tenant & City */}
          <div className="flex items-center gap-3 mt-0.5">
            <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
              <User className="h-3 w-3 flex-shrink-0" />
              <span className="truncate">
                {tenantFirstName} {tenantLastName}
              </span>
            </div>
            <span className="text-xs text-gray-400 dark:text-gray-500 truncate hidden sm:inline">
              {propertyCity}
            </span>
          </div>

          {/* Contract ID */}
          <div className="text-[10px] text-gray-400 dark:text-gray-500 mt-0.5 font-mono">
            #{contractIdentifier}
          </div>
        </div>
      </div>
    </button>
  );
};
