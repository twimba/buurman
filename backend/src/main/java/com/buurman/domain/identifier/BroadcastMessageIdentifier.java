package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class BroadcastMessageIdentifier extends Ulid {

  private BroadcastMessageIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static BroadcastMessageIdentifier of(String value) {
    return new BroadcastMessageIdentifier(value);
  }
}
