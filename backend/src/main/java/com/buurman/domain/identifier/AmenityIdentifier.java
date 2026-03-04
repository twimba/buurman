package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class AmenityIdentifier extends Ulid {

  private AmenityIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static AmenityIdentifier of(String value) {
    return new AmenityIdentifier(value);
  }
}
