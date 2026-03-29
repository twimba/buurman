package com.buurman.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/** Value type pairing a monetary amount (major units) with its ISO 4217 currency code. */
public record MoneyAmount(BigDecimal value, String currency) {

  public static MoneyAmount of(BigDecimal value, String currency) {
    int digits = CurrencyUtils.getFractionalDigits(currency);
    return new MoneyAmount(value.setScale(digits, RoundingMode.HALF_UP), currency);
  }

  public static Optional<MoneyAmount> ofNullable(
      @Nullable BigDecimal value, @Nullable String currency) {
    if (value == null || currency == null) {
      return Optional.empty();
    }
    return Optional.of(MoneyAmount.of(value, currency));
  }

  public long toMinorUnits() {
    int digits = CurrencyUtils.getFractionalDigits(currency);
    return value.movePointRight(digits).longValueExact();
  }

  /**
   * Converts a raw SUM result (minor units) back to major units using the currency's fractional
   * digits. JOOQ's {@code sum()} does not apply column converters, so aggregate results are raw
   * minor-unit values that must be converted manually.
   */
  public static BigDecimal sumToMajorUnits(
      @Nullable BigDecimal sumResult, @Nullable String currency) {
    if (sumResult == null || currency == null) {
      return BigDecimal.ZERO;
    }
    int digits = CurrencyUtils.getFractionalDigits(currency);
    return BigDecimal.valueOf(sumResult.longValueExact(), digits);
  }
}
