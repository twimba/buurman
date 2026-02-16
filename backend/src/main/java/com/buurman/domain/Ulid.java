package com.buurman.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Strongly-typed wrapper for ULID identifier strings. Serializes/deserializes as a plain string in
 * JSON, JOOQ, and Spring MVC.
 */
public record Ulid(String value) implements Comparable<Ulid>, Serializable {

  @JsonCreator
  public Ulid {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ULID value cannot be null or blank");
    }
  }

  @JsonValue
  @Override
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public int compareTo(Ulid other) {
    return this.value.compareTo(other.value);
  }

  public static Ulid of(String value) {
    return new Ulid(value);
  }
}
