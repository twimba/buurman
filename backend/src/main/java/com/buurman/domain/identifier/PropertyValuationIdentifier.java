package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyValuationIdentifier extends Ulid {

  private PropertyValuationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyValuationIdentifier of(String value) {
    return new PropertyValuationIdentifier(value);
  }
}
