package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContactRelationshipIdentifier extends Sid {

  private ContactRelationshipIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContactRelationshipIdentifier of(String value) {
    return new ContactRelationshipIdentifier(value);
  }
}
