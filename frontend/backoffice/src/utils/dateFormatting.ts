import { format } from 'date-fns';

/**
 * Returns the browser's short timezone abbreviation (e.g., "CET", "EST", "PDT").
 */
const getTimezoneAbbr = (): string => {
  const parts = new Intl.DateTimeFormat('en', {
    timeZoneName: 'short',
  }).formatToParts(new Date());
  return parts.find((p) => p.type === 'timeZoneName')?.value ?? '';
};

/**
 * Format a date string as "dd MMM yyyy HH:mm:ss TZ" (with seconds).
 */
export const formatDateTimeFull = (iso: string): string =>
  `${format(new Date(iso), 'dd MMM yyyy HH:mm:ss')} ${getTimezoneAbbr()}`;

/**
 * Format a date string as "dd MMM yyyy HH:mm TZ" (without seconds).
 */
export const formatDateTime = (iso: string): string =>
  `${format(new Date(iso), 'dd MMM yyyy HH:mm')} ${getTimezoneAbbr()}`;

/**
 * Format a date string as "dd MMM yyyy" (date only, no timezone).
 */
export const formatDate = (iso: string): string =>
  format(new Date(iso), 'dd MMM yyyy');
