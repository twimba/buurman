package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class RentRegulationRegionIdentifier extends Sid {

  private RentRegulationRegionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RentRegulationRegionIdentifier of(String value) {
    return new RentRegulationRegionIdentifier(value);
  }
}
