package com.buurman.domain;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Strongly-typed wrapper for entity identifier strings (Sid). Serializes/deserializes as a plain
 * string in JSON, JOOQ, and Spring MVC. Designed for subclassing by typed identifier classes.
 */
@Schema(
    type = "string",
    pattern = "^[A-Z]{3}[0-9A-Z]{26}$",
    example = "PRO01HQJK4B2X5M3N7P8Q9R0S1T2",
    description = "Entity-prefixed string identifier (Sid)")
public class Sid implements Comparable<Sid>, Serializable {

  private final String value;

  protected Sid(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Sid value cannot be null or blank");
    }
    this.value = value;
  }

  @JsonCreator
  public static Sid of(String value) {
    return new Sid(value);
  }

  @JsonValue
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public int compareTo(Sid other) {
    return this.value.compareTo(other.value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Sid other)) {
      return false;
    }
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }
}
