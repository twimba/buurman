package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PropertyFeeIdentifier extends Sid {

  private PropertyFeeIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyFeeIdentifier of(String value) {
    return new PropertyFeeIdentifier(value);
  }
}
