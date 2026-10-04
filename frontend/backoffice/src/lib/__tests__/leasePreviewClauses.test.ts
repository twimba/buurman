import { describe, expect, it } from 'vitest';
import {
  moveClause,
  orderedClauses,
  toChoices,
  toggleClause,
  withChoices,
} from '../leasePreviewClauses';
import type { LeasePreviewClause } from '../../generated/models';

const clause = (
  clauseKey: string,
  sortOrder: number,
  patch: Partial<LeasePreviewClause> = {}
): LeasePreviewClause => ({
  clauseKey,
  title: clauseKey,
  included: true,
  optional: false,
  pinned: false,
  articleNumber: sortOrder,
  sortOrder,
  ...patch,
});

const list = [
  clause('parties', 1, { pinned: true }),
  clause('premises', 2, { pinned: true }),
  clause('rent', 3),
  clause('pets', 4, { optional: true }),
  clause('parking', 5, { optional: true, included: false }),
];
const keys = (l: LeasePreviewClause[]) => l.map((c) => c.clauseKey);

describe('preview clause list', () => {
  it('orders by sortOrder', () => {
    expect(keys(orderedClauses([list[2], list[0], list[1]]))).toEqual([
      'parties',
      'premises',
      'rent',
    ]);
  });

  it('toggles optional clauses only', () => {
    const t = toggleClause(list, 'pets');
    expect(t.find((c) => c.clauseKey === 'pets')?.included).toBe(false);
    expect(toggleClause(list, 'rent')).toBe(list);
    expect(toggleClause(list, 'parties')).toBe(list);
    expect(toggleClause(list, 'missing')).toBe(list);
  });

  it('moves non-pinned clauses among the non-pinned', () => {
    expect(keys(moveClause(list, 'pets', -1))).toEqual([
      'parties',
      'premises',
      'pets',
      'rent',
      'parking',
    ]);
    expect(keys(moveClause(list, 'rent', 1))).toEqual([
      'parties',
      'premises',
      'pets',
      'rent',
      'parking',
    ]);
  });

  it('never moves a pinned clause or into the pinned block', () => {
    expect(moveClause(list, 'parties', 1)).toBe(list);
    expect(moveClause(list, 'rent', -1)).toBe(list);
    expect(moveClause(list, 'parking', 1)).toBe(list);
    expect(moveClause(list, 'missing', 1)).toBe(list);
  });

  it('renumbers sortOrder and keeps required clauses included', () => {
    const forced = [
      clause('a', 1),
      clause('b', 2, { included: false }),
      clause('c', 3, { optional: true, included: false }),
    ];
    expect(toChoices(forced)).toEqual([
      { clauseKey: 'a', included: true, sortOrder: 0 },
      { clauseKey: 'b', included: true, sortOrder: 1 },
      { clauseKey: 'c', included: false, sortOrder: 2 },
    ]);
  });

  it('overlays choices on a response list', () => {
    const choices = toChoices(moveClause(list, 'pets', -1));
    const merged = withChoices(list, choices);
    expect(keys(orderedClauses(merged))).toEqual([
      'parties',
      'premises',
      'pets',
      'rent',
      'parking',
    ]);
    expect(withChoices(list, null)).toBe(list);
  });
});
