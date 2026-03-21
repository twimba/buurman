package com.buurman.config.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @SuppressWarnings("NullAway")
    void convertsStringToSid() {
      Sid result = converter.from("PRO01HQJK4B2X5M3N7P8Q9R0S1T2");

      assertThat(result)
          .isNotNull()
          .extracting(Sid::value)
          .isEqualTo("PRO01HQJK4B2X5M3N7P8Q9R0S1T2");
    }

    @Test
    @DisplayName("empty string throws IllegalArgumentException")
    void emptyStringThrows() {
      assertThatThrownBy(() -> converter.from("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("blank string throws IllegalArgumentException")
    void blankStringThrows() {
      assertThatThrownBy(() -> converter.from("   ")).isInstanceOf(IllegalArgumentException.class);
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
    @SuppressWarnings("NullAway")
    void forwardRoundTrip() {
      String original = "CON01HQJK4B2X5M3N7P8Q9R0S1T2";
      Sid sid = converter.from(original);

      assertThat(sid).isNotNull();
      assertThat(converter.to(sid)).isEqualTo(original);
    }

    @Test
    @DisplayName("from(to(x)) preserves value")
    @SuppressWarnings("NullAway")
    void reverseRoundTrip() {
      Sid original = Sid.of("PRO01HQJK4B2X5M3N7P8Q9R0S1T2");
      Sid result = converter.from(converter.to(original));

      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(original);
    }
  }

  @Nested
  @DisplayName("type accessors")
  class TypeAccessors {

    @Test
    @DisplayName("fromType returns String.class")
    void fromType() {
      assertThat(converter.fromType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("toType returns Sid.class")
    void toType() {
      assertThat(converter.toType()).isEqualTo(Sid.class);
    }
  }
}
