package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContractRentPeriodIdentifier extends Sid {

  private ContractRentPeriodIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractRentPeriodIdentifier of(String value) {
    return new ContractRentPeriodIdentifier(value);
  }
}
