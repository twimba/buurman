package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class DocumentIdentifier extends Sid {

  private DocumentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DocumentIdentifier of(String value) {
    return new DocumentIdentifier(value);
  }
}
