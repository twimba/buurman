package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class OccupancyPeriodIdentifier extends Ulid {

  private OccupancyPeriodIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static OccupancyPeriodIdentifier of(String value) {
    return new OccupancyPeriodIdentifier(value);
  }
}
