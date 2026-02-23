package com.buurman.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/** Provides type-specific conversion methods for MapStruct to map nullable values to Optional. */
public class OptionalMappingConfig {

  public Optional<String> mapString(@Nullable String value) {
    return Optional.ofNullable(value);
  }

  public Optional<BigDecimal> mapBigDecimal(@Nullable BigDecimal value) {
    return Optional.ofNullable(value);
  }

  public Optional<LocalDate> mapLocalDate(@Nullable LocalDate value) {
    return Optional.ofNullable(value);
  }

  public Optional<Instant> mapInstant(@Nullable Instant value) {
    return Optional.ofNullable(value);
  }

  public Optional<Integer> mapInteger(@Nullable Integer value) {
    return Optional.ofNullable(value);
  }

  public Optional<Boolean> mapBoolean(@Nullable Boolean value) {
    return Optional.ofNullable(value);
  }

  public Optional<Long> mapLong(@Nullable Long value) {
    return Optional.ofNullable(value);
  }

  public Optional<Double> mapDouble(@Nullable Double value) {
    return Optional.ofNullable(value);
  }

  /** Generic fallback for enum types and other objects (e.g. Optional&lt;MortgageType&gt;). */
  public <T> Optional<T> mapObject(@Nullable T value) {
    return Optional.ofNullable(value);
  }
}
