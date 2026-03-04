package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyTaxIdentifier extends Ulid {

  private PropertyTaxIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyTaxIdentifier of(String value) {
    return new PropertyTaxIdentifier(value);
  }
}
