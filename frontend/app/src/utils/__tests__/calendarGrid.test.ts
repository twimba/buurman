import { describe, it, expect } from 'vitest';
import { buildMonthGrid, getWeekdayLabels } from '../calendarGrid';
import { toDayKey } from '../ics';

describe('buildMonthGrid', () => {
  it('starts every week on Monday', () => {
    const weeks = buildMonthGrid(2026, 2); // March 2026
    weeks.forEach((week) => {
      expect(week).toHaveLength(7);
      expect(week[0].date.getDay()).toBe(1);
      expect(week[6].date.getDay()).toBe(0);
    });
  });

  it('pads with the trailing days of the previous month', () => {
    // 1 March 2026 is a Sunday, so the first week runs 23 Feb – 1 Mar.
    const [firstWeek] = buildMonthGrid(2026, 2);
    expect(toDayKey(firstWeek[0].date)).toBe('2026-02-23');
    expect(firstWeek[0].inCurrentMonth).toBe(false);
    expect(toDayKey(firstWeek[6].date)).toBe('2026-03-01');
    expect(firstWeek[6].inCurrentMonth).toBe(true);
  });

  it('covers every day of the month exactly once', () => {
    const days = buildMonthGrid(2026, 2)
      .flat()
      .filter((day) => day.inCurrentMonth)
      .map((day) => day.date.getDate());
    expect(days).toEqual(Array.from({ length: 31 }, (_, i) => i + 1));
  });

  it('uses only the weeks the month touches, not a fixed six', () => {
    // February 2027 starts on a Monday and has 28 days — exactly four weeks.
    const weeks = buildMonthGrid(2027, 1);
    expect(weeks).toHaveLength(4);
    expect(toDayKey(weeks[0][0].date)).toBe('2027-02-01');
  });

  it('spans six weeks when a long month starts late in the week', () => {
    // 1 August 2026 is a Saturday; 31 days then overflow into a sixth row.
    expect(buildMonthGrid(2026, 7)).toHaveLength(6);
  });

  it('marks today, and only today', () => {
    const today = new Date(2026, 2, 15);
    const flagged = buildMonthGrid(2026, 2, today)
      .flat()
      .filter((day) => day.isToday);
    expect(flagged).toHaveLength(1);
    expect(toDayKey(flagged[0].date)).toBe('2026-03-15');
  });

  it('marks no day when today falls outside the rendered month', () => {
    const today = new Date(2026, 6, 4);
    expect(buildMonthGrid(2026, 2, today).flat().some((d) => d.isToday)).toBe(false);
  });

  it('handles a leap February', () => {
    const days = buildMonthGrid(2028, 1)
      .flat()
      .filter((day) => day.inCurrentMonth);
    expect(days).toHaveLength(29);
  });

  it('rolls December into the next year correctly', () => {
    const days = buildMonthGrid(2026, 11)
      .flat()
      .filter((day) => day.inCurrentMonth);
    expect(days).toHaveLength(31);
    expect(toDayKey(days[30].date)).toBe('2026-12-31');
  });
});

describe('getWeekdayLabels', () => {
  it('returns seven Monday-first labels', () => {
    const labels = getWeekdayLabels('en-GB');
    expect(labels).toHaveLength(7);
    expect(labels[0]).toMatch(/^Mon/);
    expect(labels[6]).toMatch(/^Sun/);
  });

  it('localises the labels', () => {
    expect(getWeekdayLabels('nl-NL')[0]).not.toBe(getWeekdayLabels('en-GB')[0]);
  });

  it('supports long labels for accessible names', () => {
    expect(getWeekdayLabels('en-GB', 'long')[0]).toBe('Monday');
  });
});
