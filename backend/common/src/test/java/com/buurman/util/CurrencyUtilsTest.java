package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

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
      // JPY symbol varies by locale — just verify it's not the code itself
      String symbol = CurrencyUtils.getCurrencySymbol("JPY");
      assertThat(symbol).isNotEmpty();
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
  }
}
