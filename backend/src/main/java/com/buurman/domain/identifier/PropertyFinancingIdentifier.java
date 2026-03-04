package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyFinancingIdentifier extends Ulid {

  private PropertyFinancingIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyFinancingIdentifier of(String value) {
    return new PropertyFinancingIdentifier(value);
  }
}
