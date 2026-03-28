package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class OccupancyPeriodIdentifier extends Sid {

  private OccupancyPeriodIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static OccupancyPeriodIdentifier of(String value) {
    return new OccupancyPeriodIdentifier(value);
  }
}
