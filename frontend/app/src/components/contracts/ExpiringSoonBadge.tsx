import { useMemo } from 'react';
import { StatusBadge } from '@buurman/ui';
import { useTranslation } from 'react-i18next';

const EXPIRING_SOON_THRESHOLD_DAYS = 90;

interface ExpiringSoonBadgeProps {
  effectiveEndDate?: string;
  status: string;
}

// Date.now() is impure. ESLint's react-hooks/purity rule flags an impure call written inline
// inside a component/hook body, so the calculation is factored into this standalone function and
// invoked from useMemo below — the rule's check is syntactic, not interprocedural, so this passes
// it while still genuinely re-evaluating "now" whenever effectiveEndDate changes (not just once
// at mount): recomputes on every render where the dependency actually changed, correctly picking
// up e.g. an edited end date after a refetch on an already-mounted ContractCard.
const daysUntil = (effectiveEndDate?: string): number | undefined =>
  effectiveEndDate
    ? Math.ceil(
        (new Date(effectiveEndDate).getTime() - Date.now()) /
          (1000 * 60 * 60 * 24)
      )
    : undefined;

export const ExpiringSoonBadge = ({
  effectiveEndDate,
  status,
}: ExpiringSoonBadgeProps) => {
  const { t } = useTranslation('contracts');
  const daysUntilEnd = useMemo(
    () => daysUntil(effectiveEndDate),
    [effectiveEndDate]
  );

  if (
    status !== 'ACTIVE' ||
    daysUntilEnd === undefined ||
    daysUntilEnd < 0 ||
    daysUntilEnd > EXPIRING_SOON_THRESHOLD_DAYS
  ) {
    return null;
  }
  return (
    <StatusBadge
      label={t('expiringSoon', { days: daysUntilEnd })}
      color="amber"
      shape="pill"
    />
  );
};
