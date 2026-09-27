package com.buurman.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.buurman.exception.BadRequestException;

/** Value type pairing a monetary amount (major units) with its ISO 4217 currency code. */
public record MoneyAmount(BigDecimal value, String currency) {

  /**
   * Bounds accepted by {@link #of}/{@link #ofNullable} before any rounding: 19 total digits (more
   * than any real money amount needs) and 6 fractional digits. Without this, a short literal with a
   * huge exponent — e.g. {@code 1E+10000000}, 11 bytes on the wire — builds a BigDecimal with
   * millions of digits before {@code setScale} ever runs, burning CPU disproportionate to the
   * request size. Jackson's number-length limit does not catch this because the exponent notation
   * itself is short.
   */
  private static final int MAX_PRECISION = 19;

  private static final int MAX_SCALE = 6;

  public static MoneyAmount of(BigDecimal value, String currency) {
    // precision()/scale() are cheap metadata reads — no digit string is materialized — so this
    // check happens BEFORE the setScale call below, which is what actually walks/rescales the
    // unscaled value and is where the cost of a huge exponent (in either direction) is paid.
    // integerDigits, not precision() alone, is what correctly bounds a value like 1E+10000000:
    // its unscaled value is a single digit, but precision() - scale() (1 - (-10000000)) reports
    // its true ~10-million-digit integer part without ever constructing that BigInteger.
    int fractionalDigits = Math.max(value.scale(), 0);
    int integerDigits = value.precision() - value.scale();
    if (fractionalDigits > MAX_SCALE || integerDigits > MAX_PRECISION) {
      throw new BadRequestException(
          "Amount is out of range: at most "
              + MAX_PRECISION
              + " total digits and "
              + MAX_SCALE
              + " decimal places are allowed.");
    }
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
