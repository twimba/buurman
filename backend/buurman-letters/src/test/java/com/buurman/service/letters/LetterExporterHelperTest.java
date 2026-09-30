package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.domain.Contract;
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

  @Test
  @DisplayName(
      "legalVariables: a catalog-configured combination resolves multiple ordered clauses with"
          + " titles")
  void legalVariablesResolvesCatalogClauses() {
    LetterExporterHelper helper =
        new LetterExporterHelper(null, null, null, null); // no repository calls on this path
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/document-extension");
    messages.setDefaultEncoding("UTF-8");
    messages.setUseCodeAsDefaultMessage(true);

    Contract contract = Contract.builder().countryCode(Optional.of("DE")).build();

    Map<String, Object> vars =
        helper.legalVariables(messages, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) vars.get("legalClauses");
    assertThat(clauses).hasSize(2);
    assertThat(clauses.get(0)).containsKey("title").containsKey("body");
  }

  @Test
  @DisplayName(
      "legalVariables: an unconfigured combination falls back to the single legacy clause, wrapped"
          + " as a one-item list")
  void legalVariablesFallsBackToSingleClause() {
    LetterExporterHelper helper = new LetterExporterHelper(null, null, null, null);
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/document-extension");
    messages.setDefaultEncoding("UTF-8");
    messages.setUseCodeAsDefaultMessage(true);

    Contract contract = Contract.builder().countryCode(Optional.of("FR")).build();

    Map<String, Object> vars =
        helper.legalVariables(messages, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) vars.get("legalClauses");
    // "rent-increase-letter" has no catalog entry for FR (only NL/DE are configured), so this
    // falls back to the legacy `legal.FR` key, which does exist in document-extension.properties
    // on this branch — resolved as a single body-only item, matching resolveLegalClause's
    // existing behavior wrapped as a one-item list.
    assertThat(clauses).hasSize(1);
    assertThat(clauses.get(0)).containsKey("body").doesNotContainKey("title");
  }

  @Test
  @DisplayName(
      "legalVariables: a contract with no country code resolves no clauses, without throwing")
  void legalVariablesHandlesMissingCountryCode() {
    LetterExporterHelper helper = new LetterExporterHelper(null, null, null, null);
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/document-extension");
    messages.setDefaultEncoding("UTF-8");
    messages.setUseCodeAsDefaultMessage(true);

    Contract contract = Contract.builder().countryCode(Optional.empty()).build();

    Map<String, Object> vars =
        helper.legalVariables(messages, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) vars.get("legalClauses");
    assertThat(clauses).isEmpty();
  }
}
