package com.buurman.domain;

import java.util.Locale;

public enum OverrideScope {
  USER,
  TEAM,
  SEGMENT;

  public String toDbValue() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static OverrideScope fromDbValue(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
