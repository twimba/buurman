package com.buurman.util;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/** Utility methods for currency validation and formatting using {@link java.util.Currency}. */
public final class CurrencyUtils {

  private CurrencyUtils() {}

  /**
   * Returns the currency symbol for the given ISO 4217 code (e.g. "USD" → "$"). Falls back to the
   * currency code itself if no symbol is found.
   */
  public static String getCurrencySymbol(@Nullable String currencyCode) {
    if (currencyCode == null || currencyCode.isBlank()) {
      return "";
    }
    try {
      Currency currency = Currency.getInstance(currencyCode);
      String symbol = currency.getSymbol();
      if (!symbol.equals(currencyCode)) {
        return symbol;
      }
      // Default locale didn't resolve the symbol — search for a locale where this currency is
      // native
      for (Locale locale : Locale.getAvailableLocales()) {
        try {
          if (currency.equals(Currency.getInstance(locale))) {
            String localeSymbol = currency.getSymbol(locale);
            if (!localeSymbol.equals(currencyCode)) {
              return localeSymbol;
            }
          }
        } catch (IllegalArgumentException ignored) {
          // Locale has no currency
        }
      }
      return symbol;
    } catch (IllegalArgumentException e) {
      return currencyCode;
    }
  }

  /**
   * Formats an amount with its currency symbol (e.g. "€1,234.56"). Returns "N/A" if amount is null.
   */
  public static String formatCurrency(@Nullable BigDecimal amount, String currencyCode) {
    if (amount == null) {
      return "N/A";
    }
    String symbol = getCurrencySymbol(currencyCode);
    int digits = getFractionalDigits(currencyCode);
    return symbol + String.format("%,." + digits + "f", amount);
  }

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
