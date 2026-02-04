import { ContractResponse } from '@/types/contract';
import { ContractStatusBadge } from './ContractStatusBadge';
import { FileText, Calendar, DollarSign } from 'lucide-react';
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
      className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm hover:shadow-md dark:hover:shadow-black/20 transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/contracts/${contract.id}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <FileText className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Contract #{contract.identifier}
              </h3>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                {contract.contractType.replace('_', ' ')}
              </p>
            </div>
          </div>
          <div className="flex-shrink-0">
            <ContractStatusBadge status={contract.status} />
          </div>
        </div>

        {/* Property and Tenant */}
        <div className="mb-3 space-y-2">
          <div>
            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">Property</p>
            <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {contract.property.street}, {contract.property.city}
            </p>
          </div>
          <div>
            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">Tenant</p>
            <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {contract.tenant.firstName} {contract.tenant.lastName}
            </p>
          </div>
        </div>

        {/* Key Information */}
        <div className="grid grid-cols-3 gap-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">Start</p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {formatDate(contract.startDate)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">End</p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {contract.endDate ? formatDate(contract.endDate) : 'Open-ended'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">Rent</p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {contract.currency} {contract.rentAmount.toFixed(2)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
