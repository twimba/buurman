import { useNavigate } from 'react-router-dom';
import { ContractStatus } from '@/types/contract';
import { Home, User } from 'lucide-react';

interface ContractCellProps {
  contractIdentifier: string;
  contractStatus: ContractStatus;
  propertyStreet: string;
  propertyCity: string;
  contactFirstName: string;
  contactLastName?: string;
  onClick?: (e: React.MouseEvent) => void;
}

const statusColors: Record<ContractStatus, string> = {
  [ContractStatus.DRAFT]: 'bg-neutral-400',
  [ContractStatus.PENDING_SIGNATURE]: 'bg-warning-text',
  [ContractStatus.ACTIVE]: 'bg-success-text',
  [ContractStatus.EXPIRED]: 'bg-warning-text',
  [ContractStatus.TERMINATED]: 'bg-error-text',
};

const statusLabels: Record<ContractStatus, string> = {
  [ContractStatus.DRAFT]: 'Draft',
  [ContractStatus.PENDING_SIGNATURE]: 'Pending',
  [ContractStatus.ACTIVE]: 'Active',
  [ContractStatus.EXPIRED]: 'Expired',
  [ContractStatus.TERMINATED]: 'Terminated',
};

export const ContractCell = ({
  contractIdentifier,
  contractStatus,
  propertyStreet,
  propertyCity,
  contactFirstName,
  contactLastName,
  onClick,
}: ContractCellProps) => {
  const navigate = useNavigate();

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onClick) {
      onClick(e);
    } else {
      navigate(`/contracts/${contractIdentifier}`);
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
            className={`w-2 h-2 rounded-full ${statusColors[contractStatus]}`}
            title={statusLabels[contractStatus]}
          />
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          {/* Property */}
          <div className="flex items-center gap-1.5 text-sm font-medium text-text-primary group-hover:text-primary-500 transition-colors">
            <Home className="h-3.5 w-3.5 text-text-muted flex-shrink-0" />
            <span className="truncate">{propertyStreet}</span>
          </div>

          {/* Contact & City */}
          <div className="flex items-center gap-3 mt-0.5">
            <div className="flex items-center gap-1 text-xs text-text-secondary">
              <User className="h-3 w-3 flex-shrink-0" />
              <span className="truncate">
                {contactFirstName} {contactLastName}
              </span>
            </div>
            <span className="text-xs text-text-muted truncate hidden sm:inline">
              {propertyCity}
            </span>
          </div>

          {/* Contract ID */}
          <div className="text-[10px] text-text-muted mt-0.5 font-mono">
            #{contractIdentifier}
          </div>
        </div>
      </div>
    </button>
  );
};
