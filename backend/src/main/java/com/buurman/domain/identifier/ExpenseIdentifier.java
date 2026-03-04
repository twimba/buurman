package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ExpenseIdentifier extends Sid {

  private ExpenseIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ExpenseIdentifier of(String value) {
    return new ExpenseIdentifier(value);
  }
}
