package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class SavedContractFilterIdentifier extends Sid {

  private SavedContractFilterIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static SavedContractFilterIdentifier of(String value) {
    return new SavedContractFilterIdentifier(value);
  }
}
