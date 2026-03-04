package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PaymentReceivalIdentifier extends Ulid {

  private PaymentReceivalIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentReceivalIdentifier of(String value) {
    return new PaymentReceivalIdentifier(value);
  }
}
