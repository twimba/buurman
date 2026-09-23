package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class DepositDeductionIdentifier extends Sid {

  private DepositDeductionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DepositDeductionIdentifier of(String value) {
    return new DepositDeductionIdentifier(value);
  }
}
