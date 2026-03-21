package com.buurman.config.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyMinorUnitConverterTest {

  private final MoneyMinorUnitConverter converter = new MoneyMinorUnitConverter();

  @Nested
  @DisplayName("from (Long -> BigDecimal)")
  class From {

    @Test
    @DisplayName("null returns null")
    void nullReturnsNull() {
      assertThat(converter.from(null)).isNull();
    }

    @Test
    @DisplayName("10012 converts to 100.12")
    void convertsMinorToMajor() {
      assertThat(converter.from(10012L)).isEqualByComparingTo(new BigDecimal("100.12"));
    }

    @Test
    @DisplayName("0 converts to 0.00")
    void zeroConverts() {
      assertThat(converter.from(0L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("negative value converts correctly")
    void negativeConverts() {
      assertThat(converter.from(-5050L)).isEqualByComparingTo(new BigDecimal("-50.50"));
    }
  }

  @Nested
  @DisplayName("to (BigDecimal -> Long)")
  class To {

    @Test
    @DisplayName("null returns null")
    void nullReturnsNull() {
      assertThat(converter.to(null)).isNull();
    }

    @Test
    @DisplayName("100.12 converts to 10012")
    void convertsMajorToMinor() {
      assertThat(converter.to(new BigDecimal("100.12"))).isEqualTo(10012L);
    }

    @Test
    @DisplayName("0.00 converts to 0")
    void zeroConverts() {
      assertThat(converter.to(new BigDecimal("0.00"))).isEqualTo(0L);
    }

    @Test
    @DisplayName("negative value converts correctly")
    void negativeConverts() {
      assertThat(converter.to(new BigDecimal("-50.50"))).isEqualTo(-5050L);
    }

    @Test
    @DisplayName("throws ArithmeticException for excess precision")
    void excessPrecisionThrows() {
      assertThatThrownBy(() -> converter.to(new BigDecimal("100.123")))
          .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("converts large value near Long.MAX_VALUE/100")
    void largeValueConverts() {
      // Long.MAX_VALUE = 9_223_372_036_854_775_807
      // Long.MAX_VALUE / 100 = 92_233_720_368_547_758.07
      BigDecimal largeValue = new BigDecimal("92233720368547758.07");
      assertThat(converter.to(largeValue)).isEqualTo(9223372036854775807L);
    }
  }

  @Nested
  @DisplayName("round-trip")
  class RoundTrip {

    @Test
    @DisplayName("from(to(x)) preserves value")
    void forwardRoundTrip() {
      BigDecimal original = new BigDecimal("123.45");
      BigDecimal result = converter.from(converter.to(original));

      assertThat(result).isEqualByComparingTo(original);
    }

    @Test
    @DisplayName("to(from(x)) preserves value")
    void reverseRoundTrip() {
      Long original = 10012L;
      Long result = converter.to(converter.from(original));

      assertThat(result).isEqualTo(original);
    }
  }

  @Nested
  @DisplayName("type accessors")
  class TypeAccessors {

    @Test
    @DisplayName("fromType returns Long.class")
    void fromType() {
      assertThat(converter.fromType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("toType returns BigDecimal.class")
    void toType() {
      assertThat(converter.toType()).isEqualTo(BigDecimal.class);
    }
  }
}
