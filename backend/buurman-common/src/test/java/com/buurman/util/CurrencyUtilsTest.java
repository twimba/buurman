package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CurrencyUtilsTest {

  @Nested
  @DisplayName("getCurrencySymbol")
  class GetCurrencySymbol {

    @Test
    @DisplayName("returns empty string for null")
    void emptyForNull() {
      assertThat(CurrencyUtils.getCurrencySymbol(null)).isEmpty();
    }

    @Test
    @DisplayName("returns empty string for blank")
    void emptyForBlank() {
      assertThat(CurrencyUtils.getCurrencySymbol("  ")).isEmpty();
    }

    @Test
    @DisplayName("returns euro sign for EUR")
    void euroSymbol() {
      assertThat(CurrencyUtils.getCurrencySymbol("EUR")).isEqualTo("\u20AC");
    }

    @Test
    @DisplayName("returns dollar sign for USD")
    void dollarSymbol() {
      assertThat(CurrencyUtils.getCurrencySymbol("USD")).isEqualTo("$");
    }

    @Test
    @DisplayName("returns yen sign for JPY")
    void yenSymbol() {
      // JPY symbol varies by locale — verify it resolved to a symbol, not the code itself
      String symbol = CurrencyUtils.getCurrencySymbol("JPY");
      assertThat(symbol).isNotEqualTo("JPY");
    }

    @Test
    @DisplayName("returns empty string for empty string")
    void emptyForEmptyString() {
      assertThat(CurrencyUtils.getCurrencySymbol("")).isEmpty();
    }

    @Test
    @DisplayName("returns the code itself for an invalid currency code")
    void invalidCode() {
      assertThat(CurrencyUtils.getCurrencySymbol("ZZZ")).isEqualTo("ZZZ");
    }
  }

  @Nested
  @DisplayName("formatCurrency")
  class FormatCurrency {

    @Test
    @DisplayName("returns N/A for null amount")
    void naForNull() {
      assertThat(CurrencyUtils.formatCurrency(null, "EUR")).isEqualTo("N/A");
    }

    @Test
    @DisplayName("formats amount with currency symbol")
    void formatsCorrectly() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1234.56"), "EUR");

      assertThat(formatted).startsWith("\u20AC");
      // Thousands separator is locale-dependent, so check digits are present
      assertThat(formatted).contains("234");
      assertThat(formatted).contains("56");
    }

    @Test
    @DisplayName("formats zero amount")
    void formatsZero() {
      String formatted = CurrencyUtils.formatCurrency(BigDecimal.ZERO, "EUR");

      assertThat(formatted).startsWith("\u20AC");
      assertThat(formatted).contains("0");
    }

    @Test
    @DisplayName("formats negative amount")
    void formatsNegative() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("-100.50"), "EUR");

      assertThat(formatted).contains("100");
      assertThat(formatted).contains("50");
    }

    @Test
    @DisplayName("formats JPY with 0 fractional digits")
    void formatsJpy() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1000"), "JPY");

      // JPY has 0 fractional digits, so no decimal point
      assertThat(formatted).doesNotContain(".");
      // Thousands separator is locale-dependent (e.g., "¥1,000"), so check key digits
      assertThat(formatted).contains("000");
    }
  }

  @Nested
  @DisplayName("getFractionalDigits")
  class GetFractionalDigits {

    @Test
    @DisplayName("returns 2 for EUR")
    void eurHasTwoDigits() {
      assertThat(CurrencyUtils.getFractionalDigits("EUR")).isEqualTo(2);
    }

    @Test
    @DisplayName("returns 2 for USD")
    void usdHasTwoDigits() {
      assertThat(CurrencyUtils.getFractionalDigits("USD")).isEqualTo(2);
    }

    @Test
    @DisplayName("returns 0 for JPY")
    void jpyHasZeroDigits() {
      assertThat(CurrencyUtils.getFractionalDigits("JPY")).isEqualTo(0);
    }

    @Test
    @DisplayName("throws for invalid currency code")
    void throwsForInvalid() {
      assertThatThrownBy(() -> CurrencyUtils.getFractionalDigits("ZZZ"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("isAmountValidForCurrency")
  class IsAmountValidForCurrency {

    @Test
    @DisplayName("10.50 is valid for EUR")
    void validEurAmount() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("10.50"), "EUR")).isTrue();
    }

    @Test
    @DisplayName("10.505 is invalid for EUR")
    void invalidEurAmount() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("10.505"), "EUR")).isFalse();
    }

    @Test
    @DisplayName("100 is valid for JPY")
    void validJpyAmount() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("100"), "JPY")).isTrue();
    }

    @Test
    @DisplayName("100.5 is invalid for JPY")
    void invalidJpyAmount() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("100.5"), "JPY")).isFalse();
    }

    @Test
    @DisplayName("whole number is valid for any currency")
    void wholeNumberValid() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("100"), "EUR")).isTrue();
    }

    @Test
    @DisplayName("exactly 2 decimal places is valid for EUR")
    void exactBoundaryValid() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("10.99"), "EUR")).isTrue();
    }
  }

  @Nested
  @DisplayName("getFractionalDigits — additional")
  class GetFractionalDigitsAdditional {

    @Test
    @DisplayName("returns 3 for BHD (3-digit currency)")
    void bhdHasThreeDigits() {
      assertThat(CurrencyUtils.getFractionalDigits("BHD")).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("isAmountValidForCurrency — additional")
  class IsAmountValidAdditional {

    @Test
    @DisplayName("1.234 is valid for BHD (3 fractional digits)")
    void validBhdAmount() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("1.234"), "BHD")).isTrue();
    }

    @Test
    @DisplayName("1.2345 is invalid for BHD")
    void invalidBhdAmount() {
      assertThat(CurrencyUtils.isAmountValidForCurrency(new BigDecimal("1.2345"), "BHD")).isFalse();
    }
  }

  @Nested
  @DisplayName("formatCurrency — additional")
  class FormatCurrencyAdditional {

    @Test
    @DisplayName("formats BHD with 3 fractional digits")
    void formatsBhdThreeDigits() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1234.567"), "BHD");

      assertThat(formatted).contains("234");
      assertThat(formatted).contains("567");
    }

    @Test
    @DisplayName("formats large amount with thousands separators")
    void formatsLargeAmount() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1234567.89"), "EUR");

      assertThat(formatted).startsWith("\u20AC");
      assertThat(formatted).contains("89");
    }
  }

  @Nested
  @DisplayName("formatCurrency \u2014 locale-aware")
  class FormatCurrencyLocale {

    private static final Locale EN = Locale.ENGLISH;
    private static final Locale DE = Locale.forLanguageTag("de");
    private static final Locale FR = Locale.forLanguageTag("fr");

    @Test
    @DisplayName("returns N/A for null amount")
    void naForNull() {
      assertThat(CurrencyUtils.formatCurrency(null, "EUR", DE)).isEqualTo("N/A");
    }

    @Test
    @DisplayName("English prefixes the symbol with dot decimals")
    void english() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1234.56"), "EUR", EN);

      assertThat(formatted).startsWith("\u20AC");
      assertThat(formatted).contains("1,234");
      assertThat(formatted).contains(".56");
    }

    @Test
    @DisplayName("German uses comma decimals, dot grouping and a trailing symbol")
    void german() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1234.56"), "EUR", DE);

      assertThat(formatted).contains("1.234");
      assertThat(formatted).contains(",56");
      assertThat(formatted).endsWith("\u20AC");
    }

    @Test
    @DisplayName("differs across locales for the same amount")
    void localesDiffer() {
      BigDecimal amount = new BigDecimal("1234.56");

      assertThat(CurrencyUtils.formatCurrency(amount, "EUR", EN))
          .isNotEqualTo(CurrencyUtils.formatCurrency(amount, "EUR", DE));
    }

    @Test
    @DisplayName("French uses comma decimals and a trailing symbol")
    void french() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("1234.56"), "EUR", FR);

      assertThat(formatted).contains(",56");
      assertThat(formatted).endsWith("\u20AC");
    }

    @Test
    @DisplayName("falls back gracefully for an unknown currency code")
    void unknownCurrency() {
      String formatted = CurrencyUtils.formatCurrency(new BigDecimal("10.00"), "ZZZ", DE);

      assertThat(formatted).contains("10");
    }
  }

  @Nested
  @DisplayName("formatNumber")
  class FormatNumber {

    @Test
    @DisplayName("returns N/A for null")
    void naForNull() {
      assertThat(CurrencyUtils.formatNumber(null, Locale.ENGLISH)).isEqualTo("N/A");
    }

    @Test
    @DisplayName("German groups with dots and decimals with comma")
    void german() {
      String formatted = CurrencyUtils.formatNumber(new BigDecimal("1234.56"), Locale.GERMAN);

      assertThat(formatted).isEqualTo("1.234,56");
    }

    @Test
    @DisplayName("English groups with commas and decimals with dot")
    void english() {
      String formatted = CurrencyUtils.formatNumber(new BigDecimal("1234.56"), Locale.ENGLISH);

      assertThat(formatted).isEqualTo("1,234.56");
    }
  }

  @Nested
  @DisplayName("formatPercent")
  class FormatPercent {

    @Test
    @DisplayName("returns N/A for null")
    void naForNull() {
      assertThat(CurrencyUtils.formatPercent(null, Locale.ENGLISH)).isEqualTo("N/A");
    }

    @Test
    @DisplayName("formats a fraction as a percentage with the locale decimal mark")
    void formatsFraction() {
      assertThat(CurrencyUtils.formatPercent(new BigDecimal("0.055"), Locale.ENGLISH))
          .contains("5.5");
      assertThat(CurrencyUtils.formatPercent(new BigDecimal("0.055"), Locale.GERMAN))
          .contains("5,5");
    }
  }
}
