package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PropertyIdentifier extends Sid {

  private PropertyIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyIdentifier of(String value) {
    return new PropertyIdentifier(value);
  }
}
