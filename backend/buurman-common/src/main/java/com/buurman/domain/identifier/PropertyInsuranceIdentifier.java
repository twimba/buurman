package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PropertyInsuranceIdentifier extends Sid {

  private PropertyInsuranceIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyInsuranceIdentifier of(String value) {
    return new PropertyInsuranceIdentifier(value);
  }
}
