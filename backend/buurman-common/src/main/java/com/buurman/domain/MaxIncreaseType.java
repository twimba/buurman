package com.buurman.domain;

public enum MaxIncreaseType {
  FIXED_PERCENTAGE,
  CPI_LINKED,
  INDEX_LINKED,
  MARKET_RENT,
  NEGOTIATED,
  FROZEN,
  /**
   * Regulatory cap defined by national/regional law (e.g. German Kappungsgrenze
   * §558 BGB capping rent uplift at 20% over 36 months). The actual percentage
   * lives in the {@code maxIncreasePercentage} field; this type signals that
   * the cap is statutory rather than a freely-chosen contractual rate.
   *
   * Seeded by migration V053 for DE Mietpreisbremse rules.
   */
  STATUTORY_CAP,
  OTHER
}
