package com.buurman.config.jooq;

import static org.assertj.core.api.Assertions.assertThat;

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
  }

  @Nested
  @DisplayName("round-trip")
  class RoundTrip {

    @Test
    @DisplayName("from(to(x)) preserves value")
    void roundTrip() {
      BigDecimal original = new BigDecimal("123.45");
      BigDecimal result = converter.from(converter.to(original));

      assertThat(result).isEqualByComparingTo(original);
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
