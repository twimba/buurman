package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class SignatureRequestIdentifier extends Sid {

  private SignatureRequestIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static SignatureRequestIdentifier of(String value) {
    return new SignatureRequestIdentifier(value);
  }
}
