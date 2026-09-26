import { startOfDay, toDayKey } from './ics';

export interface CalendarGridDay {
  date: Date;
  /** `YYYY-MM-DD`, matching the keys produced by `groupEventsByDay`. */
  key: string;
  /** False for the leading/trailing days borrowed from adjacent months. */
  inCurrentMonth: boolean;
  isToday: boolean;
}

/** Monday. Buurman calendars are week-starts-Monday throughout. */
export const WEEK_STARTS_ON = 1;

const DAYS_IN_WEEK = 7;

/**
 * Builds the day cells for a month view, padded to whole weeks.
 *
 * Only the weeks the month actually touches are returned (4–6), rather than a
 * fixed six, so short months do not render a trailing empty row.
 */
export function buildMonthGrid(
  year: number,
  month: number,
  today: Date = new Date()
): CalendarGridDay[][] {
  const firstOfMonth = new Date(year, month, 1);
  const todayKey = toDayKey(startOfDay(today));

  // How many days of the previous month are needed to reach the week start.
  const leadingDays =
    (firstOfMonth.getDay() - WEEK_STARTS_ON + DAYS_IN_WEEK) % DAYS_IN_WEEK;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const totalCells =
    Math.ceil((leadingDays + daysInMonth) / DAYS_IN_WEEK) * DAYS_IN_WEEK;

  const weeks: CalendarGridDay[][] = [];
  for (let cell = 0; cell < totalCells; cell += 1) {
    if (cell % DAYS_IN_WEEK === 0) {
      weeks.push([]);
    }
    const date = new Date(year, month, cell - leadingDays + 1);
    const key = toDayKey(date);
    weeks[weeks.length - 1].push({
      date,
      key,
      inCurrentMonth: date.getMonth() === month,
      isToday: key === todayKey,
    });
  }

  return weeks;
}

/**
 * Localised weekday labels starting on Monday, e.g. `['Mon', …, 'Sun']`.
 *
 * Derived from `Intl` so all 13 supported locales are covered without shipping
 * translated day names.
 */
export function getWeekdayLabels(
  locale: string,
  format: 'short' | 'narrow' | 'long' = 'short'
): string[] {
  const formatter = new Intl.DateTimeFormat(locale, { weekday: format });
  // 2024-01-01 was a Monday, so this walks Mon→Sun.
  return Array.from({ length: DAYS_IN_WEEK }, (_, offset) =>
    formatter.format(new Date(2024, 0, 1 + offset))
  );
}
