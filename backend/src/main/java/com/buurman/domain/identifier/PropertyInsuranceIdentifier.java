package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyInsuranceIdentifier extends Ulid {

  private PropertyInsuranceIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyInsuranceIdentifier of(String value) {
    return new PropertyInsuranceIdentifier(value);
  }
}
