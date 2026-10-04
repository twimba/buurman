package com.buurman.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum LeaseKind {
  LEGACY,
  RESIDENTIAL,
  RESIDENTIAL_FURNISHED,
  COMMERCIAL,
  MIXED_USE,
  AGRICULTURAL,
  SHORT_TERM,
  STUDENT_MOBILITY;

  /** Lowercase-hyphen form used in resource paths, e.g. {@code residential-furnished}. */
  public String pathSegment() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  /**
   * The kinds to try, most specific first, when looking up clause templates or documents for a
   * contract of this kind. The single shared definition: a furnished dwelling uses the residential
   * set until a furnished-specific one exists, and every kind ends at {@link #LEGACY}.
   */
  public List<LeaseKind> fallbackChain() {
    List<LeaseKind> chain = new ArrayList<>();
    chain.add(this);
    if (this == RESIDENTIAL_FURNISHED) {
      chain.add(RESIDENTIAL);
    }
    if (this != LEGACY) {
      chain.add(LEGACY);
    }
    return List.copyOf(chain);
  }
}
