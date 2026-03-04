package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PaymentReceivalIdentifier extends Sid {

  private PaymentReceivalIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentReceivalIdentifier of(String value) {
    return new PaymentReceivalIdentifier(value);
  }
}
