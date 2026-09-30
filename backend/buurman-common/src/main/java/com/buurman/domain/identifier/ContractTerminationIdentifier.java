package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContractTerminationIdentifier extends Sid {

  private ContractTerminationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractTerminationIdentifier of(String value) {
    return new ContractTerminationIdentifier(value);
  }
}
