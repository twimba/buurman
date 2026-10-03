package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.LeaseRegime;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.UnitResidentialDetails;

class LeaseKindResolverTest {

  private final LeaseKindResolver resolver = new LeaseKindResolver();

  private Contract contract(LeaseRegime regime) {
    return Contract.builder().leaseRegime(regime).build();
  }

  private Property property(PropertyCategory category) {
    return Property.builder().propertyCategory(category).build();
  }

  private Optional<UnitResidentialDetails> details(boolean furnished) {
    return Optional.of(UnitResidentialDetails.builder().furnished(furnished).build());
  }

  @ParameterizedTest
  @CsvSource({
    "COMMERCIAL, COMMERCIAL",
    "INDUSTRIAL, COMMERCIAL",
    "MIXED_USE, MIXED_USE",
    "AGRICULTURAL, AGRICULTURAL",
    "RESIDENTIAL, RESIDENTIAL"
  })
  void derivesKindFromCategory(PropertyCategory category, LeaseKind expected) {
    assertThat(
            resolver.resolve(contract(LeaseRegime.STANDARD), property(category), Optional.empty()))
        .isEqualTo(expected);
  }

  @Test
  void furnishedResidentialUnit() {
    assertThat(
            resolver.resolve(
                contract(LeaseRegime.STANDARD),
                property(PropertyCategory.RESIDENTIAL),
                details(true)))
        .isEqualTo(LeaseKind.RESIDENTIAL_FURNISHED);
  }

  @Test
  void unfurnishedResidentialUnit() {
    assertThat(
            resolver.resolve(
                contract(LeaseRegime.STANDARD),
                property(PropertyCategory.RESIDENTIAL),
                details(false)))
        .isEqualTo(LeaseKind.RESIDENTIAL);
  }

  @Test
  void furnishedFlagIgnoredForNonResidential() {
    assertThat(
            resolver.resolve(
                contract(LeaseRegime.STANDARD),
                property(PropertyCategory.COMMERCIAL),
                details(true)))
        .isEqualTo(LeaseKind.COMMERCIAL);
  }

  @Test
  void shortTermRegimeBeatsCategory() {
    assertThat(
            resolver.resolve(
                contract(LeaseRegime.SHORT_TERM),
                property(PropertyCategory.COMMERCIAL),
                Optional.empty()))
        .isEqualTo(LeaseKind.SHORT_TERM);
  }

  @Test
  void studentOrMobilityRegimeBeatsCategory() {
    assertThat(
            resolver.resolve(
                contract(LeaseRegime.STUDENT_OR_MOBILITY),
                property(PropertyCategory.AGRICULTURAL),
                details(true)))
        .isEqualTo(LeaseKind.STUDENT_MOBILITY);
  }

  @Test
  void nullCategoryDefaultsToResidential() {
    assertThat(
            resolver.resolve(
                contract(LeaseRegime.STANDARD), Property.builder().build(), Optional.empty()))
        .isEqualTo(LeaseKind.RESIDENTIAL);
  }
}
