package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class DocumentIdentifier extends Ulid {

  private DocumentIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static DocumentIdentifier of(String value) {
    return new DocumentIdentifier(value);
  }
}
