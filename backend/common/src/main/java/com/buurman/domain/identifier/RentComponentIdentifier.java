package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class RentComponentIdentifier extends Sid {

  private RentComponentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RentComponentIdentifier of(String value) {
    return new RentComponentIdentifier(value);
  }
}
