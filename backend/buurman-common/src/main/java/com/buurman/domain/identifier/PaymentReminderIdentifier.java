package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PaymentReminderIdentifier extends Sid {

  private PaymentReminderIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentReminderIdentifier of(String value) {
    return new PaymentReminderIdentifier(value);
  }
}
