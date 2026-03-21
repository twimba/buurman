package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class TeamIdentifier extends Sid {

  private TeamIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static TeamIdentifier of(String value) {
    return new TeamIdentifier(value);
  }
}
