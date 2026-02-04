import { PaymentResponse } from '@/types/payment';
import { PaymentStatusBadge } from './PaymentStatusBadge';
import { Receipt, Calendar, DollarSign, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useFormatDate } from '@/hooks/useFormatDate';

interface PaymentCardProps {
  payment: PaymentResponse;
}

export const PaymentCard = ({ payment }: PaymentCardProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();

  return (
    <div
      className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/payments/${payment.id}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <Receipt className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Payment #{payment.identifier}
              </h3>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Contract #{payment.contract?.identifier}
              </p>
            </div>
          </div>
          <div className="flex-shrink-0">
            <PaymentStatusBadge status={payment.status} />
          </div>
        </div>

        {/* Property and Tenant */}
        <div className="mb-3 space-y-2">
          {payment.property && (
            <div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Property
              </p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {payment.property.street}, {payment.property.city}
              </p>
            </div>
          )}
          {payment.tenant && (
            <div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">Tenant</p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {payment.tenant.firstName} {payment.tenant.lastName}
              </p>
            </div>
          )}
        </div>

        {/* Amount and Dates */}
        <div className="grid grid-cols-2 gap-3 pt-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            <div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">Amount</p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {payment.currency} {payment.amount.toFixed(2)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            <div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Due Date
              </p>
              <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                {formatDate(payment.dueDate)}
              </p>
            </div>
          </div>
        </div>

        {/* Payment Date */}
        {payment.paymentDate && (
          <div className="mt-2 pt-2 border-t border-[#edf0f7] dark:border-[#2a2e3f]">
            <div className="flex items-center gap-2 text-green-700 dark:text-green-400">
              <CheckCircle className="h-4 w-4" />
              <div>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                  Paid On
                </p>
                <p className="text-sm font-medium">
                  {formatDate(payment.paymentDate)}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
