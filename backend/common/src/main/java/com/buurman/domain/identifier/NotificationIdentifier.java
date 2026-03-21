package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.buurman.util.Generated;

@Generated
public final class NotificationIdentifier extends Sid {

  private NotificationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static NotificationIdentifier of(String value) {
    return new NotificationIdentifier(value);
  }
}
