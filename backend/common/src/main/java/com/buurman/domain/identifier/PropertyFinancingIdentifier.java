package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.buurman.util.Generated;

@Generated
public final class PropertyFinancingIdentifier extends Sid {

  private PropertyFinancingIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyFinancingIdentifier of(String value) {
    return new PropertyFinancingIdentifier(value);
  }
}
