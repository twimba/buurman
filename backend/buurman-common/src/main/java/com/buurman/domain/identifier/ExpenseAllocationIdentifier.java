package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ExpenseAllocationIdentifier extends Sid {

  private ExpenseAllocationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ExpenseAllocationIdentifier of(String value) {
    return new ExpenseAllocationIdentifier(value);
  }
}
