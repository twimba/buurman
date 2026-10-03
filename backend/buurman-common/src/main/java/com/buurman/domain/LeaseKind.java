package com.buurman.domain;

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
}
