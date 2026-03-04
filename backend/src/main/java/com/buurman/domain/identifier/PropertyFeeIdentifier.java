package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyFeeIdentifier extends Ulid {

  private PropertyFeeIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyFeeIdentifier of(String value) {
    return new PropertyFeeIdentifier(value);
  }
}
