package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.buurman.util.Generated;

@Generated
public final class TenantIdentifier extends Sid {

  private TenantIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static TenantIdentifier of(String value) {
    return new TenantIdentifier(value);
  }
}
