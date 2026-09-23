package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContactCreditIdentifier extends Sid {

  private ContactCreditIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContactCreditIdentifier of(String value) {
    return new ContactCreditIdentifier(value);
  }
}
