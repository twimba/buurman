package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class CalendarFeedIdentifier extends Sid {

  private CalendarFeedIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static CalendarFeedIdentifier of(String value) {
    return new CalendarFeedIdentifier(value);
  }
}
