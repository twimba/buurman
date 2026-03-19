import { ContractResponse } from '@/types/contract';
import { ContractStatusBadge } from './ContractStatusBadge';
import { Home, User, Calendar, DollarSign } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useFormatDate } from '@/hooks/useFormatDate';

interface ContractCardProps {
  contract: ContractResponse;
}

export const ContractCard = ({ contract }: ContractCardProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md dark:hover:shadow-black/20 transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/contracts/${contract.identifier}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="min-w-0 flex-1">
            <h3 className="text-lg font-semibold text-text-primary mb-1">
              {contract.property.street}
            </h3>
            <p className="text-sm text-text-secondary">
              #{contract.identifier}
            </p>
          </div>
          <div className="flex-shrink-0">
            <ContractStatusBadge status={contract.status} />
          </div>
        </div>

        {/* Property and Tenant */}
        <div className="mb-3 space-y-2">
          <div className="flex items-center gap-2 text-text-secondary">
            <Home className="h-4 w-4 flex-shrink-0" />
            <span className="text-sm">{contract.property.city}</span>
          </div>
          <div className="flex items-center gap-2 text-text-secondary">
            <User className="h-4 w-4 flex-shrink-0" />
            <span className="text-sm">
              {contract.primaryTenant.firstName}{' '}
              {contract.primaryTenant.lastName}
            </span>
          </div>
        </div>

        {/* Contract Type Tag */}
        <div className="mb-3">
          <span className="text-xs bg-surface-inset text-text-secondary px-2 py-1 rounded inline-block">
            {contract.contractType.replace('_', '')}
          </span>
        </div>

        {/* Key Information */}
        <div className="grid grid-cols-3 gap-3 pt-3 border-t border-border-default">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-text-muted flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-text-secondary">Start</p>
              <p className="text-sm font-medium text-text-primary">
                {formatDate(contract.startDate)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-text-muted flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-text-secondary">End</p>
              <p className="text-sm font-medium text-text-primary">
                {contract.effectiveEndDate ?? contract.endDate
                  ? formatDate((contract.effectiveEndDate ?? contract.endDate) as string)
                  : 'Open-ended'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-text-muted flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-text-secondary">Rent</p>
              <p className="text-sm font-medium text-text-primary">
                {contract.rentAmountCurrency} {contract.rentAmount.toFixed(2)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
