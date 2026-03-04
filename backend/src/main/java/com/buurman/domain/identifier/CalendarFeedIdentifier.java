package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class CalendarFeedIdentifier extends Ulid {

  private CalendarFeedIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static CalendarFeedIdentifier of(String value) {
    return new CalendarFeedIdentifier(value);
  }
}
