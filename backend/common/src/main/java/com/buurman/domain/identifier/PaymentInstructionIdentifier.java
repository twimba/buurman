package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class PaymentInstructionIdentifier extends Sid {

  private PaymentInstructionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PaymentInstructionIdentifier of(String value) {
    return new PaymentInstructionIdentifier(value);
  }
}
