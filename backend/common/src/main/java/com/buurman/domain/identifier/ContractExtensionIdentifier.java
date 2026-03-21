package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class ContractExtensionIdentifier extends Sid {

  private ContractExtensionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractExtensionIdentifier of(String value) {
    return new ContractExtensionIdentifier(value);
  }
}
