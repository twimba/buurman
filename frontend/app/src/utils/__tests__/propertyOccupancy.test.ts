import { describe, expect, it } from 'vitest';
import { describePropertyOccupancy } from '../propertyOccupancy';

// A stand-in for react-i18next's `t`: returns the defaultValue with placeholders
// interpolated, close enough to exercise the real templates without pulling i18next in.
const t = (key: string, options?: Record<string, unknown>): string => {
  const template = (options?.defaultValue as string) ?? key;
  return Object.entries(options ?? {}).reduce(
    (acc, [k, v]) =>
      k === 'defaultValue' ? acc : acc.replaceAll(`{{${k}}}`, String(v)),
    template
  );
};

describe('describePropertyOccupancy', () => {
  it('reads as "No units" when the property has none', () => {
    expect(describePropertyOccupancy(t, 0, 0, 0).label).toBe('No units');
  });

  it('single-unit property, occupied: reads as a plain natural status, not a fraction', () => {
    const result = describePropertyOccupancy(t, 1, 1, 0);
    expect(result.label).toBe('Occupied');
  });

  it('single-unit property, vacant: reads as a plain natural status', () => {
    const result = describePropertyOccupancy(t, 1, 0, 1);
    expect(result.label).toBe('Vacant');
  });

  it('single-unit property under maintenance (neither occupied nor vacant): reads as Unavailable, never Vacant', () => {
    // PREAMBLE A6: the old fraction-based logic collapsed "not occupied" into "Vacant",
    // which is false for a MAINTENANCE/UNDER_RENOVATION/etc. unit.
    const result = describePropertyOccupancy(t, 1, 0, 0);
    expect(result.label).toBe('Unavailable');
  });

  it('multi-unit, fully let', () => {
    expect(describePropertyOccupancy(t, 3, 3, 0).label).toBe('Occupied');
  });

  it('multi-unit, fully vacant', () => {
    expect(describePropertyOccupancy(t, 3, 0, 3).label).toBe('Vacant');
  });

  it('multi-unit, fully unavailable', () => {
    expect(describePropertyOccupancy(t, 2, 0, 0).label).toBe('Unavailable');
  });

  it('multi-unit mix names all three buckets rather than implying the rest are available', () => {
    // 4 units: 2 let, 1 vacant, 1 under maintenance -- occupied + vacant (3) != unitCount (4).
    const result = describePropertyOccupancy(t, 4, 2, 1);
    expect(result.label).toBe('2 let · 1 vacant · 1 unavailable');
  });
});
