import ICAL from 'ical.js';

/**
 * A single occurrence rendered by the calendar preview.
 *
 * `start` and `end` are inclusive local calendar days normalised to midnight.
 * Buurman feeds emit all-day events only (`DTSTART;VALUE=DATE`), so there is no
 * time-of-day component to preserve.
 */
export interface CalendarEvent {
  uid: string;
  title: string;
  description?: string;
  start: Date;
  end: Date;
}

/** Local midnight for a Y/M/D triple, avoiding the UTC shift `new Date(iso)` causes. */
const localDay = (year: number, month: number, day: number): Date =>
  new Date(year, month - 1, day);

/** Midnight of `date` in local time, so day comparisons ignore any time component. */
export const startOfDay = (date: Date): Date =>
  new Date(date.getFullYear(), date.getMonth(), date.getDate());

const addDays = (date: Date, days: number): Date =>
  new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);

/** `YYYY-MM-DD` in local time — a stable key for grouping events by day. */
export const toDayKey = (date: Date): string =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')}`;

/**
 * Converts an ICAL.Time to a local calendar day.
 *
 * Date-only values are read field-by-field rather than through `toJSDate()`:
 * ical.js treats them as UTC midnight, which lands on the previous day for
 * anyone west of Greenwich.
 */
function toLocalDay(time: ICAL.Time): Date {
  if (time.isDate) {
    return localDay(time.year, time.month, time.day);
  }
  return startOfDay(time.toJSDate());
}

/**
 * Parses an iCalendar document into day-oriented events.
 *
 * Scoped deliberately to the feeds Buurman generates (`CalendarFeedService`):
 * all-day `VEVENT`s with `SUMMARY`, `DESCRIPTION` and `UID`, and no `RRULE`,
 * `VTIMEZONE` or `DTEND`. Multi-day spans are still handled so a future feed
 * change degrades gracefully, but recurrence rules are not expanded — a
 * recurring event renders as its first occurrence only.
 *
 * Individual malformed events are skipped rather than failing the whole feed,
 * so one bad record cannot blank the preview.
 */
export function parseIcsFeed(ics: string): CalendarEvent[] {
  const component = new ICAL.Component(ICAL.parse(ics));
  const events: CalendarEvent[] = [];

  component.getAllSubcomponents('vevent').forEach((vevent, index) => {
    try {
      const event = new ICAL.Event(vevent);
      if (!event.startDate) {
        return;
      }

      const start = toLocalDay(event.startDate);
      let end = start;

      if (event.endDate) {
        const rawEnd = toLocalDay(event.endDate);
        // RFC 5545: DTEND is exclusive for date-only values, so step back a day
        // to get the last day the event actually covers.
        const inclusiveEnd = event.endDate.isDate
          ? addDays(rawEnd, -1)
          : rawEnd;
        if (inclusiveEnd.getTime() > start.getTime()) {
          end = inclusiveEnd;
        }
      }

      events.push({
        uid: event.uid || `event-${index}`,
        title: event.summary?.trim() || '',
        description: event.description?.trim() || undefined,
        start,
        end,
      });
    } catch {
      // Skip this event; a single unparseable record should not break the feed.
    }
  });

  return events.sort(
    (a, b) =>
      a.start.getTime() - b.start.getTime() || a.title.localeCompare(b.title)
  );
}

/**
 * Indexes events by local day key, repeating multi-day events on every day they
 * span so each grid cell can be rendered from a single lookup.
 */
export function groupEventsByDay(
  events: CalendarEvent[]
): Map<string, CalendarEvent[]> {
  const byDay = new Map<string, CalendarEvent[]>();

  events.forEach((event) => {
    for (
      let day = event.start;
      day.getTime() <= event.end.getTime();
      day = addDays(day, 1)
    ) {
      const key = toDayKey(day);
      const existing = byDay.get(key);
      if (existing) {
        existing.push(event);
      } else {
        byDay.set(key, [event]);
      }
    }
  });

  return byDay;
}
