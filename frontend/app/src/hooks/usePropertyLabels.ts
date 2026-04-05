import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

/**
 * Returns translation functions for property enum values (status, type, category).
 * Uses the `properties.enums.*` keys from locale files.
 */
export const usePropertyLabels = () => {
  const { t } = useTranslation('properties');

  const statusLabel = useCallback(
    (status: string): string => t(`enums.statuses.${status}`, status),
    [t]
  );

  const typeLabel = useCallback(
    (type: string): string => t(`enums.types.${type}`, type),
    [t]
  );

  const categoryLabel = useCallback(
    (category: string): string => t(`enums.categories.${category}`, category),
    [t]
  );

  return { statusLabel, typeLabel, categoryLabel };
};
