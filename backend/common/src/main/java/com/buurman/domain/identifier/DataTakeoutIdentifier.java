package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class DataTakeoutIdentifier extends Sid {

  private DataTakeoutIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DataTakeoutIdentifier of(String value) {
    return new DataTakeoutIdentifier(value);
  }
}
