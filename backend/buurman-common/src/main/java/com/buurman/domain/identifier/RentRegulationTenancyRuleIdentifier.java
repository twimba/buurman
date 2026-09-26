package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class RentRegulationTenancyRuleIdentifier extends Sid {

  private RentRegulationTenancyRuleIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RentRegulationTenancyRuleIdentifier of(String value) {
    return new RentRegulationTenancyRuleIdentifier(value);
  }
}
