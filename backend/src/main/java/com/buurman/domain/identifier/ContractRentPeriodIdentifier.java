package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ContractRentPeriodIdentifier extends Ulid {

  private ContractRentPeriodIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractRentPeriodIdentifier of(String value) {
    return new ContractRentPeriodIdentifier(value);
  }
}
