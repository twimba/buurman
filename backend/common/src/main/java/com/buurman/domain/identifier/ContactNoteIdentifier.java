package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContactNoteIdentifier extends Sid {

  private ContactNoteIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContactNoteIdentifier of(String value) {
    return new ContactNoteIdentifier(value);
  }
}
