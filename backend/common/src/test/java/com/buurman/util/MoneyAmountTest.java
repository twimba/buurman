package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyAmountTest {

  @Nested
  @DisplayName("of")
  class Of {

    @Test
    @DisplayName("creates MoneyAmount with given value and currency")
    void createsCorrectly() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("100.50"), "EUR");

      assertThat(money.value()).isEqualByComparingTo("100.50");
      assertThat(money.currency()).isEqualTo("EUR");
    }
  }

  @Nested
  @DisplayName("ofNullable")
  class OfNullable {

    @Test
    @DisplayName("returns empty when value is null")
    void emptyForNullValue() {
      Optional<MoneyAmount> result = MoneyAmount.ofNullable(null, "EUR");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty when currency is null")
    void emptyForNullCurrency() {
      Optional<MoneyAmount> result = MoneyAmount.ofNullable(BigDecimal.TEN, null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty when both are null")
    void emptyForBothNull() {
      Optional<MoneyAmount> result = MoneyAmount.ofNullable(null, null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns present for valid inputs")
    void presentForValidInputs() {
      Optional<MoneyAmount> result = MoneyAmount.ofNullable(new BigDecimal("50.00"), "USD");

      assertThat(result)
          .hasValueSatisfying(
              money -> {
                assertThat(money.value()).isEqualByComparingTo("50.00");
                assertThat(money.currency()).isEqualTo("USD");
              });
    }
  }

  @Nested
  @DisplayName("toMinorUnits")
  class ToMinorUnits {

    @Test
    @DisplayName("converts EUR amount to minor units (2 fractional digits)")
    void eurToMinorUnits() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("100.12"), "EUR");

      assertThat(money.toMinorUnits()).isEqualTo(10012L);
    }

    @Test
    @DisplayName("converts JPY amount to minor units (0 fractional digits)")
    void jpyToMinorUnits() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("100"), "JPY");

      assertThat(money.toMinorUnits()).isEqualTo(100L);
    }

    @Test
    @DisplayName("converts zero amount correctly")
    void zeroAmount() {
      MoneyAmount money = MoneyAmount.of(BigDecimal.ZERO, "EUR");

      assertThat(money.toMinorUnits()).isEqualTo(0L);
    }

    @Test
    @DisplayName("throws ArithmeticException for excess precision")
    void excessPrecisionThrows() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("100.123"), "EUR");

      assertThatThrownBy(money::toMinorUnits).isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("converts negative amount correctly")
    void negativeAmount() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("-50.00"), "EUR");

      assertThat(money.toMinorUnits()).isEqualTo(-5000L);
    }
  }

  @Nested
  @DisplayName("sumToMajorUnits")
  class SumToMajorUnits {

    @Test
    @DisplayName("converts minor unit sum to major units")
    void convertsSumCorrectly() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(new BigDecimal("10012"), "EUR");

      assertThat(result).isEqualByComparingTo("100.12");
    }

    @Test
    @DisplayName("returns ZERO for null sum result")
    void zeroForNullSum() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(null, "EUR");

      assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("returns ZERO for null currency")
    void zeroForNullCurrency() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(new BigDecimal("10012"), null);

      assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("returns ZERO when both are null")
    void zeroForBothNull() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(null, null);

      assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("converts JPY sum (0 fractional digits) correctly")
    void jpySumConverts() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(new BigDecimal("500"), "JPY");

      assertThat(result).isEqualByComparingTo("500");
    }
  }

  @Nested
  @DisplayName("toMinorUnits — additional")
  class ToMinorUnitsAdditional {

    @Test
    @DisplayName("converts BHD amount to minor units (3 fractional digits)")
    void bhdToMinorUnits() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("1.234"), "BHD");

      assertThat(money.toMinorUnits()).isEqualTo(1234L);
    }

    @Test
    @DisplayName("converts large EUR amount without overflow")
    void largeAmountNoOverflow() {
      MoneyAmount money = MoneyAmount.of(new BigDecimal("999999999.99"), "EUR");

      assertThat(money.toMinorUnits()).isEqualTo(99999999999L);
    }
  }

  @Nested
  @DisplayName("sumToMajorUnits — additional")
  class SumToMajorUnitsAdditional {

    @Test
    @DisplayName("converts BHD minor units (3 digits) to major")
    void bhdSumConverts() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(new BigDecimal("1234"), "BHD");

      assertThat(result).isEqualByComparingTo("1.234");
    }

    @Test
    @DisplayName("zero sum returns zero")
    void zeroSum() {
      BigDecimal result = MoneyAmount.sumToMajorUnits(BigDecimal.ZERO, "EUR");

      assertThat(result).isEqualByComparingTo("0.00");
    }
  }

  @Nested
  @DisplayName("of null arguments")
  @SuppressWarnings("NullAway")
  class OfNullArguments {

    @Test
    @DisplayName("null value is accepted by record constructor — NPE deferred to toMinorUnits()")
    void nullValueAccepted() {
      MoneyAmount money = MoneyAmount.of(null, "EUR");

      assertThat(money.value()).isNull();
      assertThatThrownBy(money::toMinorUnits).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null currency is accepted by record constructor — NPE deferred to toMinorUnits()")
    void nullCurrencyAccepted() {
      MoneyAmount money = MoneyAmount.of(BigDecimal.TEN, null);

      assertThat(money.currency()).isNull();
      assertThatThrownBy(money::toMinorUnits).isInstanceOf(NullPointerException.class);
    }
  }
}
