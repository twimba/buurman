package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PropertyAcquisitionIdentifier extends Sid {

  private PropertyAcquisitionIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PropertyAcquisitionIdentifier of(String value) {
    return new PropertyAcquisitionIdentifier(value);
  }
}
