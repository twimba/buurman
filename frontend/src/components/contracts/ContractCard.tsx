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
      className="bg-white dark:bg-gray-800 rounded-lg shadow-sm dark:shadow-gray-900 hover:shadow-md dark:hover:shadow-gray-900 transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/contracts/${contract.id}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <FileText className="h-5 w-5 text-gray-400 dark:text-gray-500 flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                Contract #{contract.identifier}
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
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
            <p className="text-xs text-gray-500 dark:text-gray-400">Property</p>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
              {contract.property.street}, {contract.property.city}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Tenant</p>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
              {contract.tenant.firstName} {contract.tenant.lastName}
            </p>
          </div>
        </div>

        {/* Key Information */}
        <div className="grid grid-cols-3 gap-3 pt-3 border-t border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-gray-400 flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-gray-500">Start</p>
              <p className="text-sm font-medium text-gray-900">
                {formatDate(contract.startDate)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-gray-400 flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-gray-500">End</p>
              <p className="text-sm font-medium text-gray-900">
                {contract.endDate ? formatDate(contract.endDate) : 'Open-ended'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-gray-400 flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-xs text-gray-500">Rent</p>
              <p className="text-sm font-medium text-gray-900">
                {contract.currency} {contract.rentAmount.toFixed(2)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
