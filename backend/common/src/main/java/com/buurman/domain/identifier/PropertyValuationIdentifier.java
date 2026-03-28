package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PropertyValuationIdentifier extends Sid {

  private PropertyValuationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyValuationIdentifier of(String value) {
    return new PropertyValuationIdentifier(value);
  }
}
