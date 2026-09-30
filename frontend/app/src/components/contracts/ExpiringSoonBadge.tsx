import { useState } from 'react';
import { StatusBadge } from '@buurman/ui';
import { useTranslation } from 'react-i18next';

const EXPIRING_SOON_THRESHOLD_DAYS = 90;

interface ExpiringSoonBadgeProps {
  effectiveEndDate?: string;
  status: string;
}

// Date.now() is impure, so it cannot be called directly in the render body (react-hooks/purity).
// A useState lazy initializer runs once per mount, outside the render-time evaluation that rule
// flags — matching the same pattern used for cooldown timers elsewhere in this codebase (see
// UserProfileSection.tsx). ContractCard always keys this by contract.identifier, so a changed
// end date remounts the badge rather than updating it in place.
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
  const [daysUntilEnd] = useState<number | undefined>(() =>
    daysUntil(effectiveEndDate)
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
