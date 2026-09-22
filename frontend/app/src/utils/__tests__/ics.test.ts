import { describe, it, expect } from 'vitest';
import { parseIcsFeed, groupEventsByDay, toDayKey } from '../ics';

/** Builds a feed in the exact shape CalendarFeedService emits. */
const feed = (...events: string[]) =>
  [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Buurman//Calendar Feed//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'X-WR-CALNAME:Buurman',
    ...events,
    'END:VCALENDAR',
  ].join('\r\n');

const allDayEvent = (uid: string, date: string, summary: string, description?: string) =>
  [
    'BEGIN:VEVENT',
    `UID:${uid}`,
    `DTSTART;VALUE=DATE:${date}`,
    `SUMMARY:${summary}`,
    ...(description ? [`DESCRIPTION:${description}`] : []),
    'STATUS:CONFIRMED',
    'TRANSP:TRANSPARENT',
    'END:VEVENT',
  ].join('\r\n');

describe('parseIcsFeed', () => {
  it('parses an all-day event into a local calendar day', () => {
    const [event] = parseIcsFeed(
      feed(allDayEvent('a@buurman', '20260315', 'Contract Start - Oak Street 12'))
    );

    expect(event.uid).toBe('a@buurman');
    expect(event.title).toBe('Contract Start - Oak Street 12');
    // The day must survive verbatim regardless of the runner's timezone.
    expect(event.start.getFullYear()).toBe(2026);
    expect(event.start.getMonth()).toBe(2);
    expect(event.start.getDate()).toBe(15);
    expect(toDayKey(event.start)).toBe('2026-03-15');
  });

  it('keeps the date stable rather than shifting it a day west of UTC', () => {
    // ical.js reads date-only values as UTC midnight; reading the fields
    // directly is what stops this becoming the 14th in negative offsets.
    const [event] = parseIcsFeed(feed(allDayEvent('b', '20260101', 'New year')));
    expect(toDayKey(event.start)).toBe('2026-01-01');
  });

  it('captures the description', () => {
    const [event] = parseIcsFeed(
      feed(allDayEvent('c', '20260401', 'Rent due', 'EUR 1200 for April'))
    );
    expect(event.description).toBe('EUR 1200 for April');
  });

  it('leaves description undefined when absent', () => {
    const [event] = parseIcsFeed(feed(allDayEvent('d', '20260401', 'Rent due')));
    expect(event.description).toBeUndefined();
  });

  it('sorts events chronologically', () => {
    const events = parseIcsFeed(
      feed(
        allDayEvent('late', '20260320', 'Later'),
        allDayEvent('early', '20260301', 'Earlier')
      )
    );
    expect(events.map((e) => e.title)).toEqual(['Earlier', 'Later']);
  });

  it('treats a single-day event as starting and ending on the same day', () => {
    const [event] = parseIcsFeed(feed(allDayEvent('e', '20260315', 'One day')));
    expect(toDayKey(event.end)).toBe('2026-03-15');
  });

  it('treats DTEND as exclusive for multi-day all-day events', () => {
    // RFC 5545: a 17th-exclusive end means the event's last day is the 16th.
    const ics = feed(
      [
        'BEGIN:VEVENT',
        'UID:span',
        'DTSTART;VALUE=DATE:20260314',
        'DTEND;VALUE=DATE:20260317',
        'SUMMARY:Inspection window',
        'END:VEVENT',
      ].join('\r\n')
    );
    const [event] = parseIcsFeed(ics);
    expect(toDayKey(event.start)).toBe('2026-03-14');
    expect(toDayKey(event.end)).toBe('2026-03-16');
  });

  it('returns an empty list for a feed with no events', () => {
    expect(parseIcsFeed(feed())).toEqual([]);
  });

  it('skips an unparseable event instead of failing the whole feed', () => {
    const ics = feed(
      ['BEGIN:VEVENT', 'UID:broken', 'SUMMARY:No start date', 'END:VEVENT'].join('\r\n'),
      allDayEvent('good', '20260315', 'Still rendered')
    );
    const events = parseIcsFeed(ics);
    expect(events).toHaveLength(1);
    expect(events[0].title).toBe('Still rendered');
  });

  it('throws on input that is not an iCalendar document', () => {
    expect(() => parseIcsFeed('<html>not a feed</html>')).toThrow();
  });
});

describe('groupEventsByDay', () => {
  it('indexes events under their day key', () => {
    const events = parseIcsFeed(
      feed(
        allDayEvent('a', '20260315', 'First'),
        allDayEvent('b', '20260315', 'Second'),
        allDayEvent('c', '20260316', 'Third')
      )
    );
    const byDay = groupEventsByDay(events);

    expect(byDay.get('2026-03-15')).toHaveLength(2);
    expect(byDay.get('2026-03-16')).toHaveLength(1);
    expect(byDay.get('2026-03-17')).toBeUndefined();
  });

  it('repeats a multi-day event on every day it covers', () => {
    const ics = feed(
      [
        'BEGIN:VEVENT',
        'UID:span',
        'DTSTART;VALUE=DATE:20260314',
        'DTEND;VALUE=DATE:20260317',
        'SUMMARY:Inspection window',
        'END:VEVENT',
      ].join('\r\n')
    );
    const byDay = groupEventsByDay(parseIcsFeed(ics));

    expect(byDay.get('2026-03-14')).toHaveLength(1);
    expect(byDay.get('2026-03-15')).toHaveLength(1);
    expect(byDay.get('2026-03-16')).toHaveLength(1);
    expect(byDay.get('2026-03-17')).toBeUndefined();
  });

  it('handles an empty event list', () => {
    expect(groupEventsByDay([]).size).toBe(0);
  });
});
