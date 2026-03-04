package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ContractPaymentInstructionIdentifier extends Ulid {

  private ContractPaymentInstructionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractPaymentInstructionIdentifier of(String value) {
    return new ContractPaymentInstructionIdentifier(value);
  }
}
