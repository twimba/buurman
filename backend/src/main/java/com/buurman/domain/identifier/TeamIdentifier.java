package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class TeamIdentifier extends Ulid {

  private TeamIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static TeamIdentifier of(String value) {
    return new TeamIdentifier(value);
  }
}
