package com.buurman.domain;

/**
 * Whether a flat late fee may be charged on overdue residential rent in a jurisdiction. Advisory
 * reference data from the rent regulation catalogue.
 */
public enum LateFeePolicy {
  /** No catalogue entry; charge as configured on the contract. */
  UNKNOWN,
  /** Contractual late fees permitted, no statutory percentage cap. */
  ALLOWED,
  /** Permitted up to a statutory maximum percentage of the amount due. */
  CAPPED,
  /** Only statutory default interest may be claimed; flat fees are not charged. */
  INTEREST_ONLY,
  /** Late fees on residential rent are prohibited. */
  FORBIDDEN
}
