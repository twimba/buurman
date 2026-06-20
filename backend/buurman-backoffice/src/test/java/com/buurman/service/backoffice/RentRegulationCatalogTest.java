package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.regulation.CatalogCountry;
import com.buurman.domain.regulation.CatalogRegion;
import com.buurman.domain.regulation.CatalogRule;
import com.buurman.domain.regulation.RentRegulationCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integrity guard for the canonical rent-regulation dataset bundled with the application. Loading
 * the file through the real {@link RentRegulationCatalogLoader} also validates that every {@code
 * maxIncreaseType} / {@code frequency} value is a known enum constant — Jackson throws on an
 * unknown name — which is the same failure the backoffice reload would otherwise hit at runtime.
 */
@DisplayName("RentRegulationCatalog (bundled dataset)")
class RentRegulationCatalogTest {

  private final RentRegulationCatalogLoader loader =
      new RentRegulationCatalogLoader(new ObjectMapper());

  @Test
  @DisplayName("bundled catalog parses, with metadata and the expected non-empty contents")
  void bundledCatalog_parsesWithMetadata() {
    RentRegulationCatalog catalog = loader.load();

    assertThat(catalog.version()).isNotBlank();
    assertThat(catalog.generatedAt()).isNotBlank();
    assertThat(catalog.countryCount()).isPositive();
    assertThat(catalog.regionCount()).isPositive();
    assertThat(catalog.ruleCount()).isPositive();
  }

  @Test
  @DisplayName("every rule carries a valid enum type and frequency (parse-enforced)")
  void everyRule_hasEnumTypeAndFrequency() {
    RentRegulationCatalog catalog = loader.load();

    for (CatalogCountry country : catalog.countries()) {
      for (CatalogRule rule : safe(country.rules())) {
        assertThat(rule.maxIncreaseType())
            .as("maxIncreaseType for %s/%d", country.countryCode(), rule.year())
            .isNotNull();
        assertThat(rule.frequency())
            .as("frequency for %s/%d", country.countryCode(), rule.year())
            .isNotNull();
      }
    }
  }

  @Test
  @DisplayName("country codes are unique and region codes are unique within a country")
  void codes_areUnique() {
    RentRegulationCatalog catalog = loader.load();

    Set<String> countryCodes = new HashSet<>();
    for (CatalogCountry country : catalog.countries()) {
      assertThat(countryCodes.add(country.countryCode()))
          .as("duplicate country code %s", country.countryCode())
          .isTrue();

      Set<String> regionCodes = new HashSet<>();
      for (CatalogRegion region : safe(country.regions())) {
        assertThat(regionCodes.add(region.regionCode()))
            .as("duplicate region code %s in %s", region.regionCode(), country.countryCode())
            .isTrue();
      }
    }
  }

  @Test
  @DisplayName("every region-scoped rule references a region declared in the same country")
  void regionScopedRules_resolveToDeclaredRegions() {
    RentRegulationCatalog catalog = loader.load();

    for (CatalogCountry country : catalog.countries()) {
      Set<String> regionCodes = new HashSet<>();
      for (CatalogRegion region : safe(country.regions())) {
        regionCodes.add(region.regionCode());
      }
      for (CatalogRule rule : safe(country.rules())) {
        if (rule.regionCode() != null) {
          assertThat(regionCodes)
              .as(
                  "rule in %s references region '%s' that is not declared",
                  country.countryCode(), rule.regionCode())
              .contains(rule.regionCode());
        }
      }
    }
  }

  private static <T> List<T> safe(List<T> list) {
    return list == null ? List.of() : list;
  }
}
