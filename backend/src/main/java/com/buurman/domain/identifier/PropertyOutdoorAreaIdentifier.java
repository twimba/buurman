package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PropertyOutdoorAreaIdentifier extends Ulid {

  private PropertyOutdoorAreaIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyOutdoorAreaIdentifier of(String value) {
    return new PropertyOutdoorAreaIdentifier(value);
  }
}
