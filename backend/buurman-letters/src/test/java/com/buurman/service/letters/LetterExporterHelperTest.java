package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Property;
import com.buurman.domain.Unit;

@DisplayName("LetterExporterHelper.premisesAddress")
class LetterExporterHelperTest {

  private static Property property(String street, String postalCode, String city) {
    return Property.builder().street(street).postalCode(postalCode).city(city).build();
  }

  private static Unit unit(String unitNumber, String name) {
    return Unit.builder().unitNumber(unitNumber).name(Optional.ofNullable(name)).build();
  }

  @Test
  @DisplayName("names the unit for a multi-unit building")
  void namesUnitForMultiUnitBuilding() {
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Keizersgracht 12", "1015 CJ", "Amsterdam"), unit("2", null), 4, "unit "))
        .isEqualTo("Keizersgracht 12, unit 2, 1015 CJ Amsterdam");
  }

  @Test
  @DisplayName("prefers the unit's own name when it has one")
  void prefersUnitName() {
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Keizersgracht 12", "1015 CJ", "Amsterdam"),
                unit("2", "Garden apartment"),
                4,
                "unit "))
        .isEqualTo("Keizersgracht 12, Garden apartment, 1015 CJ Amsterdam");
  }

  @Test
  @DisplayName("omits the unit entirely for a single-unit property")
  void omitsUnitForSingleUnitProperty() {
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Dorpsstraat 5", "3451 AB", "Utrecht"), unit("1", null), 1, "unit "))
        .isEqualTo("Dorpsstraat 5, 3451 AB Utrecht");
  }

  @Test
  @DisplayName("omits the unit designation even when the sole unit is no longer implicit")
  void omitsUnitWhenSoleUnitIsExplicit() {
    // "A" is deliberately chosen so a naive `doesNotContain` check on this address would collide
    // with the "AB" in the postal code; isEqualTo pins down the real requirement instead — the
    // address must be byte-for-byte the same single-unit address, whether or not the unit is
    // implicit.
    Unit explicitSoleUnit = unit("A", null);
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Dorpsstraat 5", "3451 AB", "Utrecht"), explicitSoleUnit, 1, "unit "))
        .isEqualTo("Dorpsstraat 5, 3451 AB Utrecht");
  }
}
