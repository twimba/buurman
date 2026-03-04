package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class TenantAddressIdentifier extends Ulid {

  private TenantAddressIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static TenantAddressIdentifier of(String value) {
    return new TenantAddressIdentifier(value);
  }
}
