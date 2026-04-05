import { useTranslation } from 'react-i18next';
import { PaymentResponse, PaymentStatus } from '@/types/payment';
import { PaymentStatusBadge } from './PaymentStatusBadge';
import { Receipt, Calendar, DollarSign, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useFormatDate } from '@/hooks/useFormatDate';
import { getCurrencySymbol } from '@/utils/currencies';

interface PaymentCardProps {
  payment: PaymentResponse;
}

export const PaymentCard = ({ payment }: PaymentCardProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const { t } = useTranslation('payments');

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
      onClick={() => navigate(`/payments/${payment.identifier}`)}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-start gap-2 min-w-0 flex-1">
            <Receipt className="h-5 w-5 text-text-muted flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <h3 className="text-lg font-semibold text-text-primary">
                {t('card.paymentId', { id: payment.identifier })}
              </h3>
              <p className="text-sm text-text-secondary">
                {t('card.contractId', { id: payment.contract?.identifier })}
              </p>
            </div>
          </div>
          <div className="flex-shrink-0">
            <PaymentStatusBadge status={payment.status} />
          </div>
        </div>

        {/* Property and Contact */}
        <div className="mb-3 space-y-2">
          {payment.property && (
            <div>
              <p className="text-xs text-text-secondary">{t('card.property')}</p>
              <p className="text-sm font-medium text-text-primary">
                {payment.property.street}, {payment.property.city}
              </p>
            </div>
          )}
          {payment.contact && (
            <div>
              <p className="text-xs text-text-secondary">{t('card.contact')}</p>
              <p className="text-sm font-medium text-text-primary">
                {payment.contact.firstName} {payment.contact.lastName}
              </p>
            </div>
          )}
        </div>

        {/* Amount and Dates */}
        <div className="grid grid-cols-2 gap-3 pt-3 border-t border-border-default">
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-text-muted " />
            <div>
              <p className="text-xs text-text-secondary">{t('card.amount')}</p>
              <p className="text-sm font-medium text-text-primary">
                {getCurrencySymbol(payment.currency)}{' '}
                {payment.amount.toFixed(2)}
              </p>
              {payment.receivedAmount > 0 &&
                payment.status !== PaymentStatus.PAID && (
                  <p className="text-xs text-text-secondary">
                    {t('card.balance', { amount: `${getCurrencySymbol(payment.currency)} ${(payment.balance ?? 0).toFixed(2)}` })}
                  </p>
                )}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-text-muted " />
            <div>
              <p className="text-xs text-text-secondary">{t('card.dueDate')}</p>
              <p className="text-sm font-medium text-text-primary">
                {formatDate(payment.dueDate)}
              </p>
            </div>
          </div>
        </div>

        {/* Payment Date */}
        {payment.paymentDate && (
          <div className="mt-2 pt-2 border-t border-border-default">
            <div className="flex items-center gap-2 text-success-text">
              <CheckCircle className="h-4 w-4" />
              <div>
                <p className="text-xs text-text-secondary">{t('card.paidOn')}</p>
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
