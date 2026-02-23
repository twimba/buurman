import { format, formatDistanceToNow } from 'date-fns';

export type DateFormatPreference = 'DD/MM/YYYY' | 'MM/DD/YYYY' | 'YYYY-MM-DD';

const DATE_FORMAT_MAP: Record<DateFormatPreference, string> = {
  'DD/MM/YYYY': 'dd/MM/yyyy',
  'MM/DD/YYYY': 'MM/dd/yyyy',
  'YYYY-MM-DD': 'yyyy-MM-dd',
};

/**
 * Convert a date to a specific timezone using Intl API,
 * then format it with date-fns using the user's preferred format.
 */
function toZonedDate(date: Date, timezone: string): Date {
  const formatted = date.toLocaleString('en-US', { timeZone: timezone });
  return new Date(formatted);
}

export function formatAppDate(
  dateInput: string | Date,
  dateFormatPref: DateFormatPreference = 'DD/MM/YYYY',
  timezone: string = 'UTC'
): string {
  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) {
    return '';
  }

  const zonedDate = toZonedDate(date, timezone);
  const fnsFormat = DATE_FORMAT_MAP[dateFormatPref] || 'dd/MM/yyyy';
  return format(zonedDate, fnsFormat);
}

export function formatAppDateTime(
  dateInput: string | Date,
  dateFormatPref: DateFormatPreference = 'DD/MM/YYYY',
  timezone: string = 'UTC'
): string {
  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) {
    return '';
  }

  const zonedDate = toZonedDate(date, timezone);
  const fnsFormat = DATE_FORMAT_MAP[dateFormatPref] || 'dd/MM/yyyy';
  return format(zonedDate, `${fnsFormat} HH:mm`);
}

export function formatAppRelativeDate(
  dateInput: string | Date,
  timezone: string = 'UTC'
): string {
  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) {
    return '';
  }

  const zonedDate = toZonedDate(date, timezone);
  return formatDistanceToNow(zonedDate, { addSuffix: true });
}
