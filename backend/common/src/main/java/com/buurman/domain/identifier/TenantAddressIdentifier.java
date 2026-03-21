package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.buurman.util.Generated;

@Generated
public final class TenantAddressIdentifier extends Sid {

  private TenantAddressIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static TenantAddressIdentifier of(String value) {
    return new TenantAddressIdentifier(value);
  }
}
