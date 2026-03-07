package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class CountryRequestIdentifier extends Sid {

  private CountryRequestIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static CountryRequestIdentifier of(String value) {
    return new CountryRequestIdentifier(value);
  }
}
