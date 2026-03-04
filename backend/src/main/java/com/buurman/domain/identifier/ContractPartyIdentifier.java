package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ContractPartyIdentifier extends Ulid {

  private ContractPartyIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractPartyIdentifier of(String value) {
    return new ContractPartyIdentifier(value);
  }
}
