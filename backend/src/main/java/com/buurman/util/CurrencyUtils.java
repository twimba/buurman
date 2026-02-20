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
}
