import { describe, expect, it } from 'vitest';
import { visibleTabs } from '../PropertyDetailPage';

describe('visibleTabs', () => {
  it('omits the Units tab for a single-unit property', () => {
    expect(visibleTabs(1)).not.toContain('units');
  });

  it('includes the Units tab once a property has more than one unit', () => {
    expect(visibleTabs(4)).toContain('units');
  });

  it('places Units directly after Info', () => {
    const tabs = visibleTabs(4);
    expect(tabs.indexOf('units')).toBe(tabs.indexOf('info') + 1);
  });

  it('keeps every pre-existing tab in its original order', () => {
    expect(visibleTabs(1)).toEqual([
      'info',
      'financials',
      'photos',
      'documents',
      'contracts',
      'expenses',
      'audit',
      'dashboard',
    ]);
  });
});
