package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PaymentPlanIdentifier extends Sid {

  private PaymentPlanIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentPlanIdentifier of(String value) {
    return new PaymentPlanIdentifier(value);
  }
}
