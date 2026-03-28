package com.buurman.domain;

import java.util.Locale;

public enum SegmentOperator {
  EQ,
  NEQ,
  IN,
  NOT_IN,
  GT,
  GTE,
  LT,
  LTE,
  CONTAINS,
  NOT_CONTAINS,
  STARTS_WITH,
  ENDS_WITH,
  REGEX;

  public String toDbValue() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static SegmentOperator fromDbValue(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
