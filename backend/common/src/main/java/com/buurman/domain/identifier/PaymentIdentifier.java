package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.buurman.util.Generated;

@Generated
public final class PaymentIdentifier extends Sid {

  private PaymentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentIdentifier of(String value) {
    return new PaymentIdentifier(value);
  }
}
