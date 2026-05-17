import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import { PaymentStatus } from '@/types/payment';

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
  className?: string;
}

const statusColors: Record<PaymentStatus, BadgeColorVariant> = {
  PENDING: 'amber',
  PARTIALLY_PAID: 'blue',
  PAID: 'emerald',
  OVERDUE: 'red',
  CANCELLED: 'gray',
};

export const PaymentStatusBadge = ({
  status,
  className = '',
}: PaymentStatusBadgeProps) => {
  const { t } = useTranslation('payments');

  const statusLabels = useMemo(
    (): Record<PaymentStatus, string> => ({
      PENDING: t('status.pending'),
      PARTIALLY_PAID: t('status.partiallyPaid'),
      PAID: t('status.paid'),
      OVERDUE: t('status.overdue'),
      CANCELLED: t('status.cancelled'),
    }),
    [t]
  );

  return (
    <StatusBadge
      label={statusLabels[status]}
      color={statusColors[status]}
      shape="pill"
      className={className}
    />
  );
};
