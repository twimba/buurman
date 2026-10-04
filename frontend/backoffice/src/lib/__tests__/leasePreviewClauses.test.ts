import { describe, expect, it } from 'vitest';
import {
  moveButtonFocus,
  moveClause,
  reconcileChoices,
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

  it('sorts pinned first even when their template sortOrder is later', () => {
    const odd = [
      clause('rent', 1),
      clause('parties', 7, { pinned: true }),
      clause('pets', 2, { optional: true }),
      clause('end', 9, { pinned: true }),
    ];
    expect(keys(orderedClauses(odd))).toEqual([
      'parties',
      'end',
      'rent',
      'pets',
    ]);
    // moving never goes into or above the pinned block
    expect(moveClause(odd, 'rent', -1)).toBe(odd);
    expect(keys(moveClause(odd, 'rent', 1))).toEqual([
      'parties',
      'end',
      'pets',
      'rent',
    ]);
    expect(moveClause(odd, 'end', 1)).toBe(odd);
  });

  it('never overlays a local sortOrder or exclusion on a pinned clause', () => {
    const odd = [clause('rent', 1), clause('parties', 7, { pinned: true })];
    const merged = withChoices(odd, [
      { clauseKey: 'parties', included: false, sortOrder: 0 },
      { clauseKey: 'rent', included: true, sortOrder: 5 },
    ]);
    const parties = merged.find((c) => c.clauseKey === 'parties');
    expect(parties?.sortOrder).toBe(7);
    expect(parties?.included).toBe(true);
    expect(keys(orderedClauses(merged))).toEqual(['parties', 'rent']);
  });

  it('drops choices for keys the latest response does not know', () => {
    const choices = [
      { clauseKey: 'pets', included: false, sortOrder: 0 },
      { clauseKey: 'gone', included: true, sortOrder: 1 },
    ];
    expect(reconcileChoices(choices, list)).toEqual([choices[0]]);
    expect(reconcileChoices([choices[1]], list)).toBeNull();
    expect(reconcileChoices(null, list)).toBeNull();
    expect(reconcileChoices(choices, [])).toBe(choices);
  });

  it('chooses which move button keeps focus after a move', () => {
    const ordered = orderedClauses(list);
    // pets is at index 3 (rent above it, parking below): both enabled
    expect(moveButtonFocus(ordered, 'pets', -1)).toBe('up');
    // rent sits right under the pinned block: up is disabled, so focus down
    expect(moveButtonFocus(ordered, 'rent', -1)).toBe('down');
    // parking is last: down is disabled, so focus up
    expect(moveButtonFocus(ordered, 'parking', 1)).toBe('up');
    expect(moveButtonFocus(ordered, 'pets', 1)).toBe('down');
  });
});
