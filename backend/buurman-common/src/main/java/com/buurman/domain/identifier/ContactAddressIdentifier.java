package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContactAddressIdentifier extends Sid {

  private ContactAddressIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContactAddressIdentifier of(String value) {
    return new ContactAddressIdentifier(value);
  }
}
