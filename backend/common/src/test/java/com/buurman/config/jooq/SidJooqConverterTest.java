package com.buurman.config.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Sid;

class SidJooqConverterTest {

  private final SidJooqConverter converter = new SidJooqConverter();

  @Nested
  @DisplayName("from (String -> Sid)")
  class From {

    @Test
    @DisplayName("null returns null")
    void nullReturnsNull() {
      assertThat(converter.from(null)).isNull();
    }

    @Test
    @DisplayName("valid string returns Sid with same value")
    void convertsStringToSid() {
      Sid result = Objects.requireNonNull(converter.from("PRO01HQJK4B2X5M3N7P8Q9R0S1T2"));

      assertThat(result.value()).isEqualTo("PRO01HQJK4B2X5M3N7P8Q9R0S1T2");
    }
  }

  @Nested
  @DisplayName("to (Sid -> String)")
  class To {

    @Test
    @DisplayName("null returns null")
    void nullReturnsNull() {
      assertThat(converter.to(null)).isNull();
    }

    @Test
    @DisplayName("Sid returns its string value")
    void convertsSidToString() {
      Sid sid = Sid.of("PRO01HQJK4B2X5M3N7P8Q9R0S1T2");

      assertThat(converter.to(sid)).isEqualTo("PRO01HQJK4B2X5M3N7P8Q9R0S1T2");
    }
  }

  @Nested
  @DisplayName("round-trip")
  class RoundTrip {

    @Test
    @DisplayName("to(from(x)) preserves value")
    void roundTrip() {
      String original = "CON01HQJK4B2X5M3N7P8Q9R0S1T2";
      Sid sid = Objects.requireNonNull(converter.from(original));

      assertThat(converter.to(sid)).isEqualTo(original);
    }
  }
}
