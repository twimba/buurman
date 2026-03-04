package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class TenantIdentifier extends Ulid {

  private TenantIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static TenantIdentifier of(String value) {
    return new TenantIdentifier(value);
  }
}
