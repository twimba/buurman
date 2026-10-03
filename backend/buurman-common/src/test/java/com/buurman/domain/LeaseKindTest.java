package com.buurman.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LeaseKindTest {

  @Test
  void furnishedFallsBackToResidentialThenLegacy() {
    assertThat(LeaseKind.RESIDENTIAL_FURNISHED.fallbackChain())
        .containsExactly(LeaseKind.RESIDENTIAL_FURNISHED, LeaseKind.RESIDENTIAL, LeaseKind.LEGACY);
  }

  @Test
  void residentialFallsBackToLegacy() {
    assertThat(LeaseKind.RESIDENTIAL.fallbackChain())
        .containsExactly(LeaseKind.RESIDENTIAL, LeaseKind.LEGACY);
  }

  @Test
  void legacyHasOnlyItself() {
    assertThat(LeaseKind.LEGACY.fallbackChain()).containsExactly(LeaseKind.LEGACY);
  }

  @ParameterizedTest
  @EnumSource(LeaseKind.class)
  void everyChainStartsWithTheKindAndEndsWithLegacy(LeaseKind kind) {
    assertThat(kind.fallbackChain()).startsWith(kind).endsWith(LeaseKind.LEGACY).doesNotHaveDuplicates();
  }
}
