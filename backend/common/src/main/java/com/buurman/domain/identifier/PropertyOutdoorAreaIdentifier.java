package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class PropertyOutdoorAreaIdentifier extends Sid {

  private PropertyOutdoorAreaIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyOutdoorAreaIdentifier of(String value) {
    return new PropertyOutdoorAreaIdentifier(value);
  }
}
