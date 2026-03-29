package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class AmenityIdentifier extends Sid {

  private AmenityIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static AmenityIdentifier of(String value) {
    return new AmenityIdentifier(value);
  }
}
