package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyIdentifier extends Ulid {

  private PropertyIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyIdentifier of(String value) {
    return new PropertyIdentifier(value);
  }
}
