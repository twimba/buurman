import type {
  LeasePreviewClause,
  LeasePreviewClauseChoice,
} from '../generated/models';

export const orderedClauses = (
  clauses: LeasePreviewClause[]
): LeasePreviewClause[] =>
  [...clauses].sort((a, b) => a.sortOrder - b.sortOrder);

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
 * or above them; an impossible move returns the same list.
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

/** Overlays the user's pending choices on the clause list the server last returned. */
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
    return choice
      ? {
          ...c,
          included: c.optional ? choice.included : true,
          sortOrder: choice.sortOrder,
        }
      : c;
  });
};
