package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class DataImportIdentifier extends Sid {

  private DataImportIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DataImportIdentifier of(String value) {
    return new DataImportIdentifier(value);
  }
}
