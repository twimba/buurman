package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContractPaymentInstructionIdentifier extends Sid {

  private ContractPaymentInstructionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractPaymentInstructionIdentifier of(String value) {
    return new ContractPaymentInstructionIdentifier(value);
  }
}
