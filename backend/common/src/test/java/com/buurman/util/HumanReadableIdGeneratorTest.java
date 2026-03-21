package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HumanReadableIdGeneratorTest {

  @Nested
  @DisplayName("generate")
  class Generate {

    @Test
    @DisplayName("returns lowercase hyphen-separated 4-part string")
    void fourParts() {
      String id = HumanReadableIdGenerator.generate();

      String[] parts = id.split("-");
      assertThat(parts).hasSize(4);
    }

    @Test
    @DisplayName("contains only lowercase letters and hyphens")
    void validCharacters() {
      String id = HumanReadableIdGenerator.generate();

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}$");
    }
  }

  @Nested
  @DisplayName("generateUnique")
  class GenerateUnique {

    @Test
    @DisplayName("returns first attempt when nothing exists")
    void returnsFirstAttempt() {
      String id = HumanReadableIdGenerator.generateUnique(code -> false);

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}$");
    }

    @Test
    @DisplayName("appends digits when all attempts collide")
    void appendsDigitsOnCollision() {
      String id = HumanReadableIdGenerator.generateUnique(code -> true);

      // After 10 collisions, appends "-NNNN"
      assertThat(id).matches("^[a-z]+(-[a-z]+){3}-\\d{4}$");
    }
  }
}
