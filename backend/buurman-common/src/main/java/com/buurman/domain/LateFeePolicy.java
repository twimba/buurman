package com.buurman.domain;

/**
 * Whether a flat late fee may be charged on overdue residential rent in a jurisdiction. Advisory
 * reference data from the rent regulation catalogue; it guides and warns, it is not legal advice.
 *
 * <p>{@link #FORBIDDEN} and {@link #INTEREST_ONLY} both mean no fee is charged. They differ in why:
 * FORBIDDEN is used where a statute or binding case law voids the clause itself, INTEREST_ONLY
 * where no such prohibition exists but only default interest and documented collection costs are
 * recoverable in practice.
 */
public enum LateFeePolicy {
  /** No catalogue entry; charge as configured on the contract. */
  UNKNOWN,
  /** Contractual late fees permitted with no statutory cap (courts may still reduce them). */
  ALLOWED,
  /**
   * Permitted but statutorily limited. {@code lateFeeMaxPercentage} carries the limit only where it
   * is a percentage of the amount due; caps expressed as a fixed sum, a multiple of the rent or a
   * per-region scale live in the notes and are not enforced automatically.
   */
  CAPPED,
  /** No prohibition, but only default interest and actual collection costs are recoverable. */
  INTEREST_ONLY,
  /** A late-fee clause in a residential lease is void or prohibited. */
  FORBIDDEN
}
