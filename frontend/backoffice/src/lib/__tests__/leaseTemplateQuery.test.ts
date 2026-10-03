import { describe, expect, it } from 'vitest';
import { leaseTemplatesKey, mergeSettled } from '../leaseTemplateQuery';

describe('mergeSettled', () => {
  const ok = <T>(value: T): PromiseSettledResult<T[]> => ({
    status: 'fulfilled',
    value: [value],
  });
  const bad: PromiseSettledResult<number[]> = {
    status: 'rejected',
    reason: new Error('x'),
  };

  it('merges all fulfilled results', () => {
    expect(mergeSettled(['NL', 'DE'], [ok(1), ok(2)])).toEqual({
      templates: [1, 2],
      failedCountries: [],
    });
  });
  it('keeps data and reports the failed country on partial failure', () => {
    expect(mergeSettled(['NL', 'DE'], [ok(1), bad])).toEqual({
      templates: [1],
      failedCountries: ['DE'],
    });
  });
  it('reports every country when all fail', () => {
    expect(mergeSettled(['NL', 'DE'], [bad, bad])).toEqual({
      templates: [],
      failedCountries: ['NL', 'DE'],
    });
  });
});

describe('leaseTemplatesKey', () => {
  it('differs by language and countries but shares the invalidation prefix', () => {
    const a = leaseTemplatesKey(['NL'], 'en');
    const b = leaseTemplatesKey(['NL'], 'de');
    const c = leaseTemplatesKey(['NL', 'DE'], 'en');
    expect(JSON.stringify(a)).not.toBe(JSON.stringify(b));
    expect(JSON.stringify(a)).not.toBe(JSON.stringify(c));
    expect(a[0]).toBe('lease-clause-templates');
    expect(b[0]).toBe(a[0]);
  });
});
