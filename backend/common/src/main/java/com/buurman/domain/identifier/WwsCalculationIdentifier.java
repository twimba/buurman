package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class WwsCalculationIdentifier extends Sid {

  private WwsCalculationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static WwsCalculationIdentifier of(String value) {
    return new WwsCalculationIdentifier(value);
  }
}
