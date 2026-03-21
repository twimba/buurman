package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class RentRegulationRuleIdentifier extends Sid {

  private RentRegulationRuleIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RentRegulationRuleIdentifier of(String value) {
    return new RentRegulationRuleIdentifier(value);
  }
}
