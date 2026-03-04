package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyAcquisitionIdentifier extends Ulid {

  private PropertyAcquisitionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyAcquisitionIdentifier of(String value) {
    return new PropertyAcquisitionIdentifier(value);
  }
}
