package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Sid;

@DisplayName("OptionalMappingConfig")
class OptionalMappingConfigTest {

  private final OptionalMappingConfig config = new OptionalMappingConfig();

  @Nested
  @DisplayName("map methods")
  class MapMethods {

    @Test
    @DisplayName("mapString wraps non-null value")
    void mapStringNonNull() {
      assertThat(config.mapString("hello")).contains("hello");
    }

    @Test
    @DisplayName("mapString returns empty for null")
    void mapStringNull() {
      assertThat(config.mapString(null)).isEmpty();
    }

    @Test
    @DisplayName("mapBigDecimal wraps non-null value")
    void mapBigDecimalNonNull() {
      assertThat(config.mapBigDecimal(BigDecimal.TEN)).contains(BigDecimal.TEN);
    }

    @Test
    @DisplayName("mapBigDecimal returns empty for null")
    void mapBigDecimalNull() {
      assertThat(config.mapBigDecimal(null)).isEmpty();
    }

    @Test
    @DisplayName("mapLocalDate wraps non-null value")
    void mapLocalDateNonNull() {
      LocalDate date = LocalDate.of(2026, 3, 1);
      assertThat(config.mapLocalDate(date)).contains(date);
    }

    @Test
    @DisplayName("mapLocalDate returns empty for null")
    void mapLocalDateNull() {
      assertThat(config.mapLocalDate(null)).isEmpty();
    }

    @Test
    @DisplayName("mapInstant wraps non-null value")
    void mapInstantNonNull() {
      Instant now = Instant.now();
      assertThat(config.mapInstant(now)).contains(now);
    }

    @Test
    @DisplayName("mapInstant returns empty for null")
    void mapInstantNull() {
      assertThat(config.mapInstant(null)).isEmpty();
    }

    @Test
    @DisplayName("mapInteger wraps non-null value")
    void mapIntegerNonNull() {
      assertThat(config.mapInteger(42)).contains(42);
    }

    @Test
    @DisplayName("mapInteger returns empty for null")
    void mapIntegerNull() {
      assertThat(config.mapInteger(null)).isEmpty();
    }

    @Test
    @DisplayName("mapBoolean wraps non-null value")
    void mapBooleanNonNull() {
      assertThat(config.mapBoolean(true)).contains(true);
    }

    @Test
    @DisplayName("mapBoolean returns empty for null")
    void mapBooleanNull() {
      assertThat(config.mapBoolean(null)).isEmpty();
    }

    @Test
    @DisplayName("mapLong wraps non-null value")
    void mapLongNonNull() {
      assertThat(config.mapLong(123L)).contains(123L);
    }

    @Test
    @DisplayName("mapLong returns empty for null")
    void mapLongNull() {
      assertThat(config.mapLong(null)).isEmpty();
    }

    @Test
    @DisplayName("mapDouble wraps non-null value")
    void mapDoubleNonNull() {
      assertThat(config.mapDouble(3.14)).contains(3.14);
    }

    @Test
    @DisplayName("mapDouble returns empty for null")
    void mapDoubleNull() {
      assertThat(config.mapDouble(null)).isEmpty();
    }

    @Test
    @DisplayName("mapObject wraps non-null value")
    void mapObjectNonNull() {
      assertThat(config.mapObject("test")).contains("test");
    }

    @Test
    @DisplayName("mapObject returns empty for null")
    void mapObjectNull() {
      assertThat(config.<String>mapObject(null)).isEmpty();
    }
  }

  @Nested
  @DisplayName("Sid map methods")
  class SidMapMethods {

    @Test
    @DisplayName("mapStringToOptionalSid wraps non-null string as Sid")
    void mapStringToSidNonNull() {
      Optional<Sid> result = config.mapStringToOptionalSid("ABC01HQJK4B2X5M3N7P8Q9R0S1T2");
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(Sid.of("ABC01HQJK4B2X5M3N7P8Q9R0S1T2"));
    }

    @Test
    @DisplayName("mapStringToOptionalSid returns empty for null")
    void mapStringToSidNull() {
      assertThat(config.mapStringToOptionalSid(null)).isEmpty();
    }

    @Test
    @DisplayName("mapSid wraps Sid in Optional")
    void mapSid() {
      Sid sid = Sid.of("ABC01HQJK4B2X5M3N7P8Q9R0S1T2");
      assertThat(config.mapSid(sid)).contains(sid);
    }
  }

  @Nested
  @DisplayName("unwrap methods")
  class UnwrapMethods {

    @Test
    @DisplayName("unwrapString returns value when present")
    void unwrapStringPresent() {
      assertThat(config.unwrapString(Optional.of("hello"))).isEqualTo("hello");
    }

    @Test
    @DisplayName("unwrapString returns null when empty")
    void unwrapStringEmpty() {
      assertThat(config.unwrapString(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapBigDecimal returns value when present")
    void unwrapBigDecimalPresent() {
      assertThat(config.unwrapBigDecimal(Optional.of(BigDecimal.TEN))).isEqualTo(BigDecimal.TEN);
    }

    @Test
    @DisplayName("unwrapBigDecimal returns null when empty")
    void unwrapBigDecimalEmpty() {
      assertThat(config.unwrapBigDecimal(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapLocalDate returns value when present")
    void unwrapLocalDatePresent() {
      LocalDate date = LocalDate.of(2026, 3, 1);
      assertThat(config.unwrapLocalDate(Optional.of(date))).isEqualTo(date);
    }

    @Test
    @DisplayName("unwrapLocalDate returns null when empty")
    void unwrapLocalDateEmpty() {
      assertThat(config.unwrapLocalDate(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapInteger returns value when present")
    void unwrapIntegerPresent() {
      assertThat(config.unwrapInteger(Optional.of(42))).isEqualTo(42);
    }

    @Test
    @DisplayName("unwrapInteger returns null when empty")
    void unwrapIntegerEmpty() {
      assertThat(config.unwrapInteger(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapDouble returns value when present")
    void unwrapDoublePresent() {
      assertThat(config.unwrapDouble(Optional.of(3.14))).isEqualTo(3.14);
    }

    @Test
    @DisplayName("unwrapDouble returns null when empty")
    void unwrapDoubleEmpty() {
      assertThat(config.unwrapDouble(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapBoolean returns value when present")
    void unwrapBooleanPresent() {
      assertThat(config.unwrapBoolean(Optional.of(true))).isTrue();
    }

    @Test
    @DisplayName("unwrapBoolean returns null when empty")
    void unwrapBooleanEmpty() {
      assertThat(config.unwrapBoolean(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapLong returns value when present")
    void unwrapLongPresent() {
      assertThat(config.unwrapLong(Optional.of(123L))).isEqualTo(123L);
    }

    @Test
    @DisplayName("unwrapLong returns null when empty")
    void unwrapLongEmpty() {
      assertThat(config.unwrapLong(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapInstant returns value when present")
    void unwrapInstantPresent() {
      Instant now = Instant.now();
      assertThat(config.unwrapInstant(Optional.of(now))).isEqualTo(now);
    }

    @Test
    @DisplayName("unwrapInstant returns null when empty")
    void unwrapInstantEmpty() {
      assertThat(config.unwrapInstant(Optional.empty())).isNull();
    }

    @Test
    @DisplayName("unwrapObject returns value when present")
    void unwrapObjectPresent() {
      assertThat(config.unwrapObject(Optional.of("test"))).isEqualTo("test");
    }

    @Test
    @DisplayName("unwrapObject returns null when empty")
    void unwrapObjectEmpty() {
      assertThat((Object) config.unwrapObject(Optional.empty())).isNull();
    }
  }

  @Nested
  @DisplayName("Sid unwrap methods")
  class SidUnwrapMethods {

    @Test
    @DisplayName("unwrapSid returns Sid when present")
    void unwrapSidPresent() {
      Sid sid = Sid.of("ABC01HQJK4B2X5M3N7P8Q9R0S1T2");
      assertThat(config.unwrapSid(Optional.of(sid))).isEqualTo(sid);
    }

    @Test
    @DisplayName("unwrapSid throws when empty")
    void unwrapSidEmpty() {
      assertThatThrownBy(() -> config.unwrapSid(Optional.empty()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Sid identifier must be present");
    }

    @Test
    @DisplayName("unwrapOptionalSid returns Sid when present")
    void unwrapOptionalSidPresent() {
      Sid sid = Sid.of("ABC01HQJK4B2X5M3N7P8Q9R0S1T2");
      assertThat(config.unwrapOptionalSid(Optional.of(sid))).isEqualTo(sid);
    }

    @Test
    @DisplayName("unwrapOptionalSid returns null when empty")
    void unwrapOptionalSidEmpty() {
      assertThat(config.unwrapOptionalSid(Optional.empty())).isNull();
    }
  }
}
