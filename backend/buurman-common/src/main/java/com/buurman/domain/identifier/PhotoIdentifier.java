package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class PhotoIdentifier extends Sid {

  private PhotoIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static PhotoIdentifier of(String value) {
    return new PhotoIdentifier(value);
  }
}
