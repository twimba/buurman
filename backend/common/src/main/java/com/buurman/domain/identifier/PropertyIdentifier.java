package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class PropertyIdentifier extends Sid {

  private PropertyIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyIdentifier of(String value) {
    return new PropertyIdentifier(value);
  }
}
