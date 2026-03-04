package com.buurman.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Sid;

/** Provides type-specific conversion methods for MapStruct to map nullable values to Optional. */
@Component
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

  public Optional<Sid> mapStringToOptionalSid(@Nullable String value) {
    return Optional.ofNullable(value).map(Sid::of);
  }

  public Optional<Sid> mapSid(Sid value) {
    return Optional.of(value);
  }

  /** Generic fallback for enum types and other objects (e.g. Optional&lt;MortgageType&gt;). */
  public <T> Optional<T> mapObject(@Nullable T value) {
    return Optional.ofNullable(value);
  }

  // Unwrappers: Optional<T> → @Nullable T, used by update mappers so that
  // NullValuePropertyMappingStrategy.IGNORE can skip empty Optionals (they unwrap to null).

  public @Nullable String unwrapString(Optional<String> value) {
    return value.orElse(null);
  }

  public @Nullable BigDecimal unwrapBigDecimal(Optional<BigDecimal> value) {
    return value.orElse(null);
  }

  public @Nullable LocalDate unwrapLocalDate(Optional<LocalDate> value) {
    return value.orElse(null);
  }

  public @Nullable Integer unwrapInteger(Optional<Integer> value) {
    return value.orElse(null);
  }

  public @Nullable Double unwrapDouble(Optional<Double> value) {
    return value.orElse(null);
  }

  public @Nullable Boolean unwrapBoolean(Optional<Boolean> value) {
    return value.orElse(null);
  }

  public @Nullable Long unwrapLong(Optional<Long> value) {
    return value.orElse(null);
  }

  public @Nullable Instant unwrapInstant(Optional<Instant> value) {
    return value.orElse(null);
  }

  public Sid unwrapSid(Optional<Sid> value) {
    return value.orElseThrow(() -> new IllegalStateException("Sid identifier must be present"));
  }

  public @Nullable Sid unwrapOptionalSid(Optional<Sid> value) {
    return value.orElse(null);
  }

  public <T> @Nullable T unwrapObject(Optional<T> value) {
    return value.orElse(null);
  }
}
