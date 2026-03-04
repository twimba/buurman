package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ExpenseIdentifier extends Ulid {

  private ExpenseIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ExpenseIdentifier of(String value) {
    return new ExpenseIdentifier(value);
  }
}
