import { describe, expect, it } from 'vitest';
import type { LeaseClauseTemplateResponse } from '../../generated/models';
import {
  applyFilters,
  behaviourFlags,
  groupTemplates,
  hasProblem,
  isEnglishFallback,
  isUnresolved,
  DEFAULT_FILTERS,
} from '../leaseClauseTable';

const make = (
  over: Partial<LeaseClauseTemplateResponse> = {}
): LeaseClauseTemplateResponse => ({
  identifier: 'id',
  countryCode: 'NL',
  leaseKind: 'RESIDENTIAL',
  clauseKey: 'parties',
  titleI18nKey: 't.key',
  bodyI18nKey: 'b.key',
  defaultIncluded: true,
  optional: true,
  pinned: false,
  sortOrder: 10,
  version: 1,
  titleText: 'Partijen',
  bodyText: 'Tekst',
  missingLanguages: [],
  ...over,
});

describe('isUnresolved', () => {
  it('is true when the text equals the key', () => {
    expect(isUnresolved('t.key', 't.key')).toBe(true);
    expect(isUnresolved('Partijen', 't.key')).toBe(false);
  });
});

describe('isEnglishFallback', () => {
  it('flags non-English languages listed as missing', () => {
    const t = make({ missingLanguages: ['nl'] });
    expect(isEnglishFallback(t, 'nl')).toBe(true);
    expect(isEnglishFallback(t, 'de')).toBe(false);
  });
  it('never flags English itself', () => {
    expect(isEnglishFallback(make({ missingLanguages: ['en'] }), 'en')).toBe(
      false
    );
  });
});

describe('hasProblem', () => {
  it('detects missing translation in the selected language', () => {
    expect(hasProblem(make({ missingLanguages: ['nl'] }), 'nl')).toBe(true);
    expect(hasProblem(make({ missingLanguages: ['nl'] }), 'de')).toBe(false);
  });
  it('detects unresolved title or body', () => {
    expect(hasProblem(make({ titleText: 't.key' }), 'en')).toBe(true);
    expect(hasProblem(make({ bodyText: 'b.key' }), 'en')).toBe(true);
  });
  it('is false for a healthy row', () => {
    expect(hasProblem(make(), 'nl')).toBe(false);
  });
});

describe('behaviourFlags', () => {
  it('shows nothing for optional default-on', () => {
    expect(behaviourFlags(make())).toEqual([]);
  });
  it('lists required, pinned and default off', () => {
    expect(behaviourFlags(make({ optional: false, pinned: true }))).toEqual([
      'required',
      'pinned',
    ]);
    expect(behaviourFlags(make({ defaultIncluded: false }))).toEqual([
      'defaultOff',
    ]);
  });
  it('does not flag default off on required clauses', () => {
    expect(
      behaviourFlags(make({ optional: false, defaultIncluded: false }))
    ).toEqual(['required']);
  });
});

describe('groupTemplates', () => {
  it('groups by country alphabetically then kind in enum order, rows by sortOrder', () => {
    const rows = [
      make({ identifier: '1', countryCode: 'NL', leaseKind: 'COMMERCIAL' }),
      make({
        identifier: '2',
        countryCode: 'DE',
        leaseKind: 'RESIDENTIAL',
        sortOrder: 20,
      }),
      make({ identifier: '3', countryCode: 'NL', leaseKind: 'LEGACY' }),
      make({
        identifier: '4',
        countryCode: 'DE',
        leaseKind: 'RESIDENTIAL',
        sortOrder: 5,
      }),
    ];
    const groups = groupTemplates(rows);
    expect(groups.map((g) => g.countryCode)).toEqual(['DE', 'NL']);
    expect(groups[1].kinds.map((k) => k.kind)).toEqual([
      'LEGACY',
      'COMMERCIAL',
    ]);
    expect(groups[0].kinds[0].rows.map((r) => r.identifier)).toEqual([
      '4',
      '2',
    ]);
    expect(groups[0].count).toBe(2);
  });
  it('returns an empty list for no rows', () => {
    expect(groupTemplates([])).toEqual([]);
  });
});

describe('applyFilters', () => {
  const rows = [
    make({ identifier: 'a', leaseKind: 'RESIDENTIAL' }),
    make({ identifier: 'b', leaseKind: 'COMMERCIAL', pinned: true }),
    make({ identifier: 'c', leaseKind: 'COMMERCIAL', optional: false }),
    make({
      identifier: 'd',
      leaseKind: 'COMMERCIAL',
      missingLanguages: ['nl'],
    }),
  ];
  const ids = (r: LeaseClauseTemplateResponse[]) => r.map((x) => x.identifier);

  it('passes everything by default', () => {
    expect(applyFilters(rows, DEFAULT_FILTERS, 'nl')).toHaveLength(4);
  });
  it('filters by kind', () => {
    expect(
      ids(applyFilters(rows, { ...DEFAULT_FILTERS, kind: 'RESIDENTIAL' }, 'en'))
    ).toEqual(['a']);
  });
  it('filters by behaviour', () => {
    expect(
      ids(applyFilters(rows, { ...DEFAULT_FILTERS, behaviour: 'pinned' }, 'en'))
    ).toEqual(['b']);
    expect(
      ids(
        applyFilters(rows, { ...DEFAULT_FILTERS, behaviour: 'required' }, 'en')
      )
    ).toEqual(['c']);
    expect(
      ids(
        applyFilters(rows, { ...DEFAULT_FILTERS, behaviour: 'optional' }, 'en')
      )
    ).toEqual(['a', 'b', 'd']);
  });
  it('composes problems-only with kind', () => {
    expect(
      ids(
        applyFilters(
          rows,
          { ...DEFAULT_FILTERS, kind: 'COMMERCIAL', problemsOnly: true },
          'nl'
        )
      )
    ).toEqual(['d']);
  });
});

describe('applyFilters country', () => {
  it('filters by country', () => {
    const rows = [
      make({ identifier: 'x', countryCode: 'NL' }),
      make({ identifier: 'y', countryCode: 'DE' }),
    ];
    expect(
      applyFilters(rows, { ...DEFAULT_FILTERS, country: 'DE' }, 'en').map(
        (r) => r.identifier
      )
    ).toEqual(['y']);
  });
});
