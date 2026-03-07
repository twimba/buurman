package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class RentRegulationCountryIdentifier extends Sid {

  private RentRegulationCountryIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RentRegulationCountryIdentifier of(String value) {
    return new RentRegulationCountryIdentifier(value);
  }
}
