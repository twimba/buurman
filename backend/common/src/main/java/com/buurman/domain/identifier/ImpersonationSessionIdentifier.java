package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class ImpersonationSessionIdentifier extends Sid {

  private ImpersonationSessionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ImpersonationSessionIdentifier of(String value) {
    return new ImpersonationSessionIdentifier(value);
  }
}
