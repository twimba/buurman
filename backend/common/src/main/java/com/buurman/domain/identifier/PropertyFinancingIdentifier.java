package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

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
