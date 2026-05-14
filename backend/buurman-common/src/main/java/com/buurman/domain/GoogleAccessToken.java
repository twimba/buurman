package com.buurman.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * User-supplied OAuth 2.0 access token used to authorize a single Google Sheets export. Holds the
 * raw token transiently for the duration of one request; never persisted, never serialized. The
 * {@code value} accessor is marked {@link JsonIgnore} so Jackson cannot accidentally emit the token
 * via reflection-based serialization.
 */
public record GoogleAccessToken(@JsonIgnore String value) {
  public GoogleAccessToken {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Google access token must not be blank");
    }
  }

  @Override
  public String toString() {
    return "GoogleAccessToken[REDACTED]";
  }
}
