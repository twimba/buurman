import { describe, expect, it } from 'vitest';
import type { ResolvedLeaseClauseResponse } from '@/generated/models';
import {
  isLeaseSelectionDirty,
  orderClauses,
  serverClauseOrder,
} from '../leaseClauseSelection';

const clause = (
  id: string,
  sortOrder: number,
  extra: Partial<ResolvedLeaseClauseResponse> = {}
): ResolvedLeaseClauseResponse => ({
  templateIdentifier: id,
  clauseKey: id,
  title: id,
  body: '',
  included: true,
  optional: true,
  sortOrder,
  pinned: false,
  articleNumber: sortOrder,
  ...extra,
});

const PARTIES = clause('parties', 1, { pinned: true, optional: false });
const PETS = clause('pets', 2, { included: false });
const PARKING = clause('parking', 3);
const CLAUSES = [PARKING, PETS, PARTIES];

describe('serverClauseOrder / orderClauses', () => {
  it('puts pinned clauses first, then sorts by sortOrder', () => {
    expect(serverClauseOrder(CLAUSES).map((c) => c.templateIdentifier)).toEqual(
      ['parties', 'pets', 'parking']
    );
  });

  it('applies a local order and appends clauses it has not seen', () => {
    expect(
      orderClauses(CLAUSES, ['parties', 'parking']).map(
        (c) => c.templateIdentifier
      )
    ).toEqual(['parties', 'parking', 'pets']);
  });
});

describe('isLeaseSelectionDirty', () => {
  it('is clean without overrides or a local order', () => {
    expect(isLeaseSelectionDirty(CLAUSES, {}, null)).toBe(false);
  });

  it('is dirty when an optional clause is toggled away from the server state', () => {
    expect(isLeaseSelectionDirty(CLAUSES, { pets: true }, null)).toBe(true);
    expect(isLeaseSelectionDirty(CLAUSES, { parking: false }, null)).toBe(true);
  });

  it('is clean when an override equals the server state (toggled back)', () => {
    expect(isLeaseSelectionDirty(CLAUSES, { pets: false }, null)).toBe(false);
  });

  it('ignores overrides on required clauses and unknown clauses', () => {
    expect(
      isLeaseSelectionDirty(CLAUSES, { parties: false, gone: true }, null)
    ).toBe(false);
  });

  it('is dirty when the local order differs from the server order', () => {
    expect(
      isLeaseSelectionDirty(CLAUSES, {}, ['parties', 'parking', 'pets'])
    ).toBe(true);
  });

  it('is clean when the local order equals the server order (moved back)', () => {
    expect(
      isLeaseSelectionDirty(CLAUSES, {}, ['parties', 'pets', 'parking'])
    ).toBe(false);
  });

  it('is clean for an empty clause list', () => {
    expect(isLeaseSelectionDirty([], {}, [])).toBe(false);
  });
});
