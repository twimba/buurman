package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContactIdentifier extends Sid {

  private ContactIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContactIdentifier of(String value) {
    return new ContactIdentifier(value);
  }
}
