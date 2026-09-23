import { useTranslation } from 'react-i18next';
import { StatusBadge } from '@buurman/ui';
import type { PaymentType } from '@/types/payment';

interface PaymentTypeBadgeProps {
  type?: PaymentType;
  className?: string;
}

/** Only non-rent types get a badge; regular rent stays unlabelled to keep lists quiet. */
export const PaymentTypeBadge = ({ type, className }: PaymentTypeBadgeProps) => {
  const { t } = useTranslation('payments');
  if (!type || type === 'RENT') {
    return null;
  }
  return (
    <StatusBadge
      label={t(`types.${type}`)}
      color="amber"
      shape="pill"
      className={className}
    />
  );
};
