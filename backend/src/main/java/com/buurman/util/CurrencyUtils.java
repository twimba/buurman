package com.buurman.util;

import java.math.BigDecimal;
import java.util.Currency;

/** Utility methods for currency validation using {@link java.util.Currency}. */
public final class CurrencyUtils {

  private CurrencyUtils() {}

  /**
   * Returns the number of fractional digits for the given ISO 4217 currency code.
   *
   * @throws IllegalArgumentException if the currency code is unknown
   */
  public static int getFractionalDigits(String currencyCode) {
    return Currency.getInstance(currencyCode).getDefaultFractionDigits();
  }

  /**
   * Validates that the given amount does not have more decimal places than the currency allows.
   *
   * @return true if valid
   */
  public static boolean isAmountValidForCurrency(BigDecimal amount, String currencyCode) {
    int allowed = getFractionalDigits(currencyCode);
    return amount.stripTrailingZeros().scale() <= allowed;
  }

  /** Converts a major-unit BigDecimal amount to minor-unit long (e.g. 100.12 EUR → 10012). */
  public static long toMinorUnits(BigDecimal amount, String currencyCode) {
    int digits = getFractionalDigits(currencyCode);
    return amount.movePointRight(digits).longValueExact();
  }

  /** Null-safe variant of {@link #toMinorUnits}. */
  public static Long toMinorUnitsOrNull(BigDecimal amount, String currencyCode) {
    return amount == null ? null : toMinorUnits(amount, currencyCode);
  }

  /** Converts a minor-unit long to major-unit BigDecimal (e.g. 10012, EUR → 100.12). */
  public static BigDecimal toMajorUnits(long minorUnits, String currencyCode) {
    int digits = getFractionalDigits(currencyCode);
    return BigDecimal.valueOf(minorUnits, digits);
  }

  /** Null-safe variant of {@link #toMajorUnits}. */
  public static BigDecimal toMajorUnitsOrNull(Long minorUnits, String currencyCode) {
    return minorUnits == null ? null : toMajorUnits(minorUnits, currencyCode);
  }

  /**
   * Converts a SUM(BIGINT) result (returned as BigDecimal by JOOQ) back to major units. Returns
   * {@link BigDecimal#ZERO} when the sum is null.
   */
  public static BigDecimal sumToMajorUnits(BigDecimal sumResult, String currencyCode) {
    if (sumResult == null) return BigDecimal.ZERO;
    return toMajorUnits(sumResult.longValueExact(), currencyCode);
  }
}
