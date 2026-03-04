package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class FinancingPaymentIdentifier extends Ulid {

  private FinancingPaymentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static FinancingPaymentIdentifier of(String value) {
    return new FinancingPaymentIdentifier(value);
  }
}
