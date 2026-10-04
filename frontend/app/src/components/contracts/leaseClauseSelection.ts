import type { ResolvedLeaseClauseResponse } from '@/generated/models';

/** Deviations from the server's `included`, keyed by templateIdentifier (optional clauses only). */
export type ClauseOverrides = Record<string, boolean>;

/** Local clause order as templateIdentifiers; null means "the server order". */
export type ClauseOrder = string[] | null;

export const isClauseIncluded = (
  clause: ResolvedLeaseClauseResponse,
  overrides: ClauseOverrides
): boolean =>
  clause.optional
    ? (overrides[clause.templateIdentifier] ?? clause.included)
    : true;

/** Server order: pinned clauses (parties, premises) lead, the rest follow by sortOrder. */
export const serverClauseOrder = (
  clauses: ResolvedLeaseClauseResponse[]
): ResolvedLeaseClauseResponse[] =>
  clauses
    .slice()
    .sort(
      (a, b) => Number(b.pinned) - Number(a.pinned) || a.sortOrder - b.sortOrder
    );

/** The clauses as displayed: the local order when there is one, else the server order. */
export const orderClauses = (
  clauses: ResolvedLeaseClauseResponse[],
  order: ClauseOrder
): ResolvedLeaseClauseResponse[] => {
  const base = serverClauseOrder(clauses);
  if (!order) {
    return base;
  }
  const byId = new Map(base.map((c) => [c.templateIdentifier, c]));
  const known = new Set(order);
  const ordered = order
    .map((id) => byId.get(id))
    .filter((c): c is ResolvedLeaseClauseResponse => !!c);
  // A refetch may add clauses the local order has never seen; keep them in server order.
  const unseen = base.filter((c) => !known.has(c.templateIdentifier));
  return [...ordered, ...unseen].sort(
    (a, b) => Number(b.pinned) - Number(a.pinned)
  );
};

/**
 * Whether the local selection differs from what the server has saved: an optional clause whose
 * effective inclusion differs from `included`, or a display order different from the server
 * order. Toggling a clause back, or moving it back, is therefore not a change.
 */
export const isLeaseSelectionDirty = (
  clauses: ResolvedLeaseClauseResponse[],
  overrides: ClauseOverrides,
  order: ClauseOrder
): boolean => {
  const toggled = clauses.some(
    (clause) =>
      clause.optional && isClauseIncluded(clause, overrides) !== clause.included
  );
  if (toggled) {
    return true;
  }
  if (!order) {
    return false;
  }
  const server = serverClauseOrder(clauses);
  const local = orderClauses(clauses, order);
  return local.some(
    (clause, index) =>
      clause.templateIdentifier !== server[index].templateIdentifier
  );
};
