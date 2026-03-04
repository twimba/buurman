package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class DataTakeoutIdentifier extends Ulid {

  private DataTakeoutIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DataTakeoutIdentifier of(String value) {
    return new DataTakeoutIdentifier(value);
  }
}
