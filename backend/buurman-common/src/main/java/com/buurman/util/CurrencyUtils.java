package com.buurman.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
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
   *
   * @deprecated Locale-unaware: always uses US-style grouping and prefixes the symbol, so German
   *     renders {@code €1.234,56} as {@code €1,234.56}. Prefer {@link #formatCurrency(BigDecimal,
   *     String, Locale)} so grouping separators, decimal marks and symbol placement follow the
   *     reader's locale.
   */
  @Deprecated
  public static String formatCurrency(@Nullable BigDecimal amount, String currencyCode) {
    if (amount == null) {
      return "N/A";
    }
    String symbol = getCurrencySymbol(currencyCode);
    int digits = getFractionalDigits(currencyCode);
    String format = "%,." + digits + "f";
    return symbol + String.format(format, amount);
  }

  /**
   * Formats an amount in its currency for the given locale, following CLDR rules for grouping
   * separators, decimal marks and currency-symbol placement.
   *
   * <p>Examples for {@code EUR 1234.56}: {@code €1,234.56} (en), {@code 1.234,56 €} (de), {@code 1
   * 234,56 €} (fr). Returns "N/A" if amount is null. Falls back to the locale-unaware {@link
   * #formatCurrency(BigDecimal, String)} if the currency code is unknown.
   */
  public static String formatCurrency(
      @Nullable BigDecimal amount, String currencyCode, Locale locale) {
    if (amount == null) {
      return "N/A";
    }
    try {
      Currency currency = Currency.getInstance(currencyCode);
      NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
      nf.setCurrency(currency);
      int digits = currency.getDefaultFractionDigits();
      nf.setMinimumFractionDigits(digits);
      nf.setMaximumFractionDigits(digits);
      return nf.format(amount);
    } catch (IllegalArgumentException e) {
      // Unknown ISO 4217 code — degrade to symbol (or the code) + a locale-formatted number.
      return getCurrencySymbol(currencyCode) + formatNumber(amount, locale);
    }
  }

  /** Formats a plain decimal for the given locale (e.g. {@code 1.234,56} in de). */
  public static String formatNumber(@Nullable BigDecimal value, Locale locale) {
    if (value == null) {
      return "N/A";
    }
    NumberFormat nf = NumberFormat.getNumberInstance(locale);
    nf.setMaximumFractionDigits(Math.max(2, value.scale()));
    return nf.format(value);
  }

  /**
   * Formats a fraction as a locale-aware percentage. Pass the fraction, not the percentage: {@code
   * 0.055} renders as {@code 5.5%} (en) / {@code 5,5 %} (de). Returns "N/A" if null.
   */
  public static String formatPercent(@Nullable BigDecimal fraction, Locale locale) {
    if (fraction == null) {
      return "N/A";
    }
    NumberFormat nf = NumberFormat.getPercentInstance(locale);
    nf.setMaximumFractionDigits(2);
    return nf.format(fraction);
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
