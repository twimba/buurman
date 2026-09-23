package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class DepositIdentifier extends Sid {

  private DepositIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DepositIdentifier of(String value) {
    return new DepositIdentifier(value);
  }
}
