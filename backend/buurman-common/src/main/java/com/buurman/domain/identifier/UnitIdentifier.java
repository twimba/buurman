package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class UnitIdentifier extends Sid {

  private UnitIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static UnitIdentifier of(String value) {
    return new UnitIdentifier(value);
  }
}
