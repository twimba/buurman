import type {
  LeasePreviewClause,
  LeasePreviewClauseChoice,
} from '../generated/models';

/** Backend order: pinned first, then by sortOrder (the backend ignores a pinned sortOrder). */
export const orderedClauses = (
  clauses: LeasePreviewClause[]
): LeasePreviewClause[] =>
  [...clauses].sort(
    (a, b) => Number(!a.pinned) - Number(!b.pinned) || a.sortOrder - b.sortOrder
  );

/** Only optional clauses can be switched off; anything else returns the same list. */
export const toggleClause = (
  clauses: LeasePreviewClause[],
  clauseKey: string
): LeasePreviewClause[] => {
  const target = clauses.find((c) => c.clauseKey === clauseKey);
  if (!target || !target.optional || target.pinned) {
    return clauses;
  }
  return clauses.map((c) =>
    c.clauseKey === clauseKey ? { ...c, included: !c.included } : c
  );
};

/**
 * Swaps a non-pinned clause with its neighbour. Pinned clauses never move and nothing moves into
 * or above the pinned block; an impossible move returns the same list.
 */
export const moveClause = (
  clauses: LeasePreviewClause[],
  clauseKey: string,
  direction: -1 | 1
): LeasePreviewClause[] => {
  const ordered = orderedClauses(clauses);
  const index = ordered.findIndex((c) => c.clauseKey === clauseKey);
  const neighbour = ordered[index + direction];
  if (index < 0 || ordered[index].pinned || !neighbour || neighbour.pinned) {
    return clauses;
  }
  const next = [...ordered];
  [next[index], next[index + direction]] = [neighbour, ordered[index]];
  return next.map((c, i) => ({ ...c, sortOrder: i + 1 }));
};

/** The request representation: contiguous sort orders, required clauses always included. */
export const toChoices = (
  clauses: LeasePreviewClause[]
): LeasePreviewClauseChoice[] =>
  orderedClauses(clauses).map((c, i) => ({
    clauseKey: c.clauseKey,
    included: c.optional ? c.included : true,
    sortOrder: i,
  }));

/**
 * Overlays the user's choices (toggles and the order of optional clauses) on the clause list the
 * server last returned. Pinned and required clauses are never overridden.
 */
export const withChoices = (
  clauses: LeasePreviewClause[],
  choices: LeasePreviewClauseChoice[] | null
): LeasePreviewClause[] => {
  if (!choices) {
    return clauses;
  }
  const byKey = new Map(choices.map((c) => [c.clauseKey, c]));
  return clauses.map((c) => {
    const choice = byKey.get(c.clauseKey);
    if (!choice || c.pinned) {
      return c;
    }
    return {
      ...c,
      included: c.optional ? choice.included : true,
      sortOrder: choice.sortOrder,
    };
  });
};

/** Drops choices whose clause is not in the latest response (e.g. after a country change). */
export const reconcileChoices = (
  choices: LeasePreviewClauseChoice[] | null,
  clauses: LeasePreviewClause[]
): LeasePreviewClauseChoice[] | null => {
  if (!choices || clauses.length === 0) {
    return choices;
  }
  const known = new Set(clauses.map((c) => c.clauseKey));
  const kept = choices.filter((c) => known.has(c.clauseKey));
  if (kept.length === choices.length) {
    return choices;
  }
  return kept.length > 0 ? kept : null;
};

export type MoveButton = 'up' | 'down';

/**
 * After moving `clauseKey` in `direction`, which of its buttons should keep keyboard focus:
 * the one just used, or the opposite one when the clause now sits at an edge.
 */
export const moveButtonFocus = (
  ordered: LeasePreviewClause[],
  clauseKey: string,
  direction: -1 | 1
): MoveButton => {
  const index = ordered.findIndex((c) => c.clauseKey === clauseKey);
  const canUp = !!ordered[index - 1] && !ordered[index - 1].pinned;
  const canDown = !!ordered[index + 1];
  const wanted: MoveButton = direction === -1 ? 'up' : 'down';
  if (wanted === 'up') {
    return canUp || !canDown ? 'up' : 'down';
  }
  return canDown || !canUp ? 'down' : 'up';
};
