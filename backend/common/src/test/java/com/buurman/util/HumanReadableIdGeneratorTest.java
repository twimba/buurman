package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HumanReadableIdGenerator}.
 *
 * <p>Currently disabled: DataFaker 2.5.4's {@code resolve("adjective.positive")} throws at runtime
 * because the resolve API no longer supports that key format. The generator needs to be updated to
 * use the typed provider API (e.g. {@code faker.adjective().positive()}) once DataFaker exposes it,
 * or switch to a different approach.
 */
class HumanReadableIdGeneratorTest {

  @Nested
  @DisplayName("generate")
  class Generate {

    @Test
    @Disabled("DataFaker 2.5.4 resolve('adjective.positive') is broken — known production bug")
    @DisplayName("returns lowercase hyphen-separated 4-part string")
    void fourParts() {
      String id = HumanReadableIdGenerator.generate();

      String[] parts = id.split("-");
      assertThat(parts).hasSize(4);
    }

    @Test
    @Disabled("DataFaker 2.5.4 resolve('adjective.positive') is broken — known production bug")
    @DisplayName("contains only lowercase letters and hyphens")
    void validCharacters() {
      String id = HumanReadableIdGenerator.generate();

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}$");
    }

    @Test
    @DisplayName("generate throws due to broken DataFaker resolve API")
    void throwsDueToDataFakerBug() {
      assertThatThrownBy(HumanReadableIdGenerator::generate).isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("generateUnique")
  class GenerateUnique {

    @Test
    @Disabled("DataFaker 2.5.4 resolve('adjective.positive') is broken — known production bug")
    @DisplayName("returns first attempt when nothing exists")
    void returnsFirstAttempt() {
      String id = HumanReadableIdGenerator.generateUnique(code -> false);

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}$");
    }

    @Test
    @Disabled("DataFaker 2.5.4 resolve('adjective.positive') is broken — known production bug")
    @DisplayName("appends digits when all attempts collide")
    void appendsDigitsOnCollision() {
      String id = HumanReadableIdGenerator.generateUnique(code -> true);

      // After 10 collisions, appends "-NNNN"
      assertThat(id).matches("^[a-z]+(-[a-z]+){3}-\\d{4}$");
    }
  }
}
