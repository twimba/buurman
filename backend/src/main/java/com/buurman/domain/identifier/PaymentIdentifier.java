package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PaymentIdentifier extends Ulid {

  private PaymentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentIdentifier of(String value) {
    return new PaymentIdentifier(value);
  }
}
