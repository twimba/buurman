package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ContractPartyIdentifier extends Sid {

  private ContractPartyIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractPartyIdentifier of(String value) {
    return new ContractPartyIdentifier(value);
  }
}
