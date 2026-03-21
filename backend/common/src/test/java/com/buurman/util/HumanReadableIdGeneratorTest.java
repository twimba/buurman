package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

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
      AtomicInteger callCount = new AtomicInteger(0);

      String id =
          HumanReadableIdGenerator.generateUnique(
              code -> {
                callCount.incrementAndGet();
                return false;
              });

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}$");
      assertThat(callCount).hasValue(1);
    }

    @Test
    @DisplayName("appends digits when all attempts collide")
    void appendsDigitsOnCollision() {
      AtomicInteger callCount = new AtomicInteger(0);

      String id =
          HumanReadableIdGenerator.generateUnique(
              code -> {
                callCount.incrementAndGet();
                return true;
              });

      // After 10 collisions, appends "-NNNN"
      assertThat(id).matches("^[a-z]+(-[a-z]+){3}-\\d{4}$");
      assertThat(callCount).hasValue(10);
    }

    @Test
    @DisplayName("returns ID without suffix after partial collisions")
    void partialCollision() {
      AtomicInteger callCount = new AtomicInteger(0);

      String id =
          HumanReadableIdGenerator.generateUnique(
              code -> {
                // First 5 calls return true (collision), 6th returns false (available)
                return callCount.incrementAndGet() <= 5;
              });

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}$");
      assertThat(callCount).hasValue(6);
    }

    @Test
    @DisplayName("falls back to digit suffix after exactly 10 collisions")
    void fullCollisionExhaustsRetries() {
      AtomicInteger callCount = new AtomicInteger(0);

      String id =
          HumanReadableIdGenerator.generateUnique(
              code -> {
                callCount.incrementAndGet();
                return true;
              });

      assertThat(id).matches("^[a-z]+(-[a-z]+){3}-\\d{4}$");
      assertThat(callCount).hasValue(10);
    }
  }
}
