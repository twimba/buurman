package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class PaymentInstructionIdentifier extends Ulid {

  private PaymentInstructionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentInstructionIdentifier of(String value) {
    return new PaymentInstructionIdentifier(value);
  }
}
