package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class FinancingPaymentIdentifier extends Sid {

  private FinancingPaymentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static FinancingPaymentIdentifier of(String value) {
    return new FinancingPaymentIdentifier(value);
  }
}
