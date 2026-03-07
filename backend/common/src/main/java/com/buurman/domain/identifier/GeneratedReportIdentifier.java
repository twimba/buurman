package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class GeneratedReportIdentifier extends Sid {

  private GeneratedReportIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static GeneratedReportIdentifier of(String value) {
    return new GeneratedReportIdentifier(value);
  }
}
