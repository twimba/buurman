package com.buurman.domain;

/**
 * Frequency at which a rent change can be applied.
 *
 * <p>Both {@code SEMI_ANNUAL} and {@code SEMIANNUAL} are present because existing contracts use the
 * underscore form (legacy domain code) while migration V053__rent_regulation_overhaul.sql seeds the
 * no-underscore form for some regulatory rules. They are aliases — same semantic, both must be
 * parseable.
 *
 * <p>{@code ON_RELET} and {@code PER_MODERNISATION} are seeded by V053 for regimes whose increases
 * trigger on specific events rather than on a cadence — used by NL/AT new-let rules and DE §559 BGB
 * respectively.
 */
public enum RentFrequency {
  MONTHLY,
  QUARTERLY,
  SEMI_ANNUAL,
  SEMIANNUAL,
  ANNUAL,
  BIENNIAL,
  TRIENNIAL,
  ON_RELET,
  PER_MODERNISATION
}
