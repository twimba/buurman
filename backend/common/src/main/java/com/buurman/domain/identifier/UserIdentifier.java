package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.buurman.util.Generated;

@Generated
public final class UserIdentifier extends Sid {

  private UserIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static UserIdentifier of(String value) {
    return new UserIdentifier(value);
  }
}
