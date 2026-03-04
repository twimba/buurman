package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class UserIdentifier extends Ulid {

  private UserIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static UserIdentifier of(String value) {
    return new UserIdentifier(value);
  }
}
