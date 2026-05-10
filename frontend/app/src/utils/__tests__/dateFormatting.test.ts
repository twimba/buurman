import {
  formatAppDate,
  formatAppDateTime,
  formatAppRelativeDate,
} from '../dateFormatting';

describe('formatAppDate', () => {
  it('formats a date string with default DD/MM/YYYY format', () => {
    expect(formatAppDate('2026-03-15T10:30:00Z')).toBe('15/03/2026');
  });

  it('formats with MM/DD/YYYY preference', () => {
    expect(formatAppDate('2026-03-15T10:30:00Z', 'MM/DD/YYYY')).toBe(
      '03/15/2026'
    );
  });

  it('formats with YYYY-MM-DD preference', () => {
    expect(formatAppDate('2026-03-15T10:30:00Z', 'YYYY-MM-DD')).toBe(
      '2026-03-15'
    );
  });

  it('accepts a Date object', () => {
    const date = new Date('2026-01-01T00:00:00Z');
    expect(formatAppDate(date, 'YYYY-MM-DD', 'UTC')).toBe('2026-01-01');
  });

  it('returns empty string for invalid date string', () => {
    expect(formatAppDate('not-a-date')).toBe('');
  });

  it('respects timezone parameter', () => {
    // Midnight UTC on Jan 1 is Dec 31 in US timezones
    const result = formatAppDate(
      '2026-01-01T00:30:00Z',
      'YYYY-MM-DD',
      'America/New_York'
    );
    // 00:30 UTC = 19:30 Dec 31 EST
    expect(result).toBe('2025-12-31');
  });
});

describe('formatAppDateTime', () => {
  it('includes time in HH:mm format', () => {
    const result = formatAppDateTime(
      '2026-06-20T14:45:00Z',
      'DD/MM/YYYY',
      'UTC'
    );
    expect(result).toBe('20/06/2026 14:45');
  });

  it('formats with MM/DD/YYYY preference including time', () => {
    const result = formatAppDateTime(
      '2026-06-20T08:05:00Z',
      'MM/DD/YYYY',
      'UTC'
    );
    expect(result).toBe('06/20/2026 08:05');
  });

  it('returns empty string for invalid date', () => {
    expect(formatAppDateTime('garbage')).toBe('');
  });

  it('adjusts time for timezone', () => {
    // 14:00 UTC = 16:00 CEST (Europe/Amsterdam in summer)
    const result = formatAppDateTime(
      '2026-06-20T14:00:00Z',
      'DD/MM/YYYY',
      'Europe/Amsterdam'
    );
    expect(result).toBe('20/06/2026 16:00');
  });
});

describe('formatAppRelativeDate', () => {
  it('returns a relative time string with "ago" suffix', () => {
    const recentDate = new Date(Date.now() - 5 * 60 * 1000).toISOString();
    const result = formatAppRelativeDate(recentDate, 'UTC');
    // The exact wording depends on toZonedDate conversion, but it always ends with "ago"
    expect(result).toContain('ago');
  });

  it('returns empty string for invalid date', () => {
    expect(formatAppRelativeDate('invalid')).toBe('');
  });
});
