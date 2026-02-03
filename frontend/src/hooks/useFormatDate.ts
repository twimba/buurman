import { useCallback } from 'react';
import { useUserPreferences } from './useUserPreferencesHooks';
import {
  formatAppDate,
  formatAppDateTime,
  formatAppRelativeDate,
  DateFormatPreference,
} from '../utils/dateFormatting';

export const useFormatDate = () => {
  const { data: preferences } = useUserPreferences();

  const dateFormat =
    (preferences?.dateFormat as DateFormatPreference) || 'DD/MM/YYYY';
  const timezone = preferences?.timezone || 'UTC';

  const formatDate = useCallback(
    (dateInput: string | Date) =>
      formatAppDate(dateInput, dateFormat, timezone),
    [dateFormat, timezone]
  );

  const formatDateTime = useCallback(
    (dateInput: string | Date) =>
      formatAppDateTime(dateInput, dateFormat, timezone),
    [dateFormat, timezone]
  );

  const formatRelative = useCallback(
    (dateInput: string | Date) => formatAppRelativeDate(dateInput, timezone),
    [timezone]
  );

  return { formatDate, formatDateTime, formatRelative };
};
