package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ImpersonationSessionIdentifier extends Sid {

  private ImpersonationSessionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ImpersonationSessionIdentifier of(String value) {
    return new ImpersonationSessionIdentifier(value);
  }
}
