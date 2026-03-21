package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class ContractIdentifier extends Sid {

  private ContractIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractIdentifier of(String value) {
    return new ContractIdentifier(value);
  }
}
