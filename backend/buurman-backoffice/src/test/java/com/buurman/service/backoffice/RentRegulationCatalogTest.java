package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.domain.regulation.CatalogCountry;
import com.buurman.domain.regulation.CatalogRegion;
import com.buurman.domain.regulation.CatalogRule;
import com.buurman.domain.regulation.CatalogTenancyRule;
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
  @DisplayName("every country declares a late-fee regime; capped ones with a percentage carry it")
  void everyCountry_hasLateFeeRegime() {
    RentRegulationCatalog catalog = loader.load();

    for (CatalogCountry country : catalog.countries()) {
      assertThat(country.lateFee()).as("lateFee for %s", country.countryCode()).isNotNull();
      assertThat(country.lateFee().policy())
          .as("lateFee.policy for %s", country.countryCode())
          .isNotNull()
          .isNotEqualTo(LateFeePolicy.UNKNOWN);
      assertThat(country.formalNoticeDays())
          .as("formalNoticeDays for %s", country.countryCode())
          .isNotNull()
          .isBetween(1, 365);
      if (country.lateFee().maxPercentage() != null) {
        assertThat(country.lateFee().policy()).isEqualTo(LateFeePolicy.CAPPED);
        assertThat(country.lateFee().maxPercentage()).isPositive();
      }
    }
    assertThat(catalog.countries())
        .anySatisfy(
            c -> {
              assertThat(c.countryCode()).isEqualTo("PT");
              assertThat(c.lateFee().policy()).isEqualTo(LateFeePolicy.CAPPED);
              // art. 1041 CC as amended by Lei 13/2019; the older 50% figure was repealed.
              assertThat(c.lateFee().maxPercentage()).isEqualByComparingTo("20");
            });
  }

  @Test
  @DisplayName("jurisdictions that void late-fee clauses are recorded as FORBIDDEN")
  void prohibitionsAreRecorded() {
    Map<String, LateFeePolicy> byCode =
        loader.load().countries().stream()
            .collect(Collectors.toMap(CatalogCountry::countryCode, c -> c.lateFee().policy()));

    // FR loi 89-462 art. 4 i); PL art. 483 § 1 KC; NO husleieloven § 3-7; DE § 555 BGB;
    // NL unfair-terms nullity (CJEU C-488/11).
    assertThat(byCode)
        .containsEntry("FR", LateFeePolicy.FORBIDDEN)
        .containsEntry("PL", LateFeePolicy.FORBIDDEN)
        .containsEntry("NO", LateFeePolicy.FORBIDDEN)
        .containsEntry("DE", LateFeePolicy.FORBIDDEN)
        .containsEntry("NL", LateFeePolicy.FORBIDDEN);
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

  @Test
  @DisplayName("every tenancy rule carries a topic, label and value, and resolves its region")
  void tenancyRules_areWellFormed() {
    RentRegulationCatalog catalog = loader.load();

    for (CatalogCountry country : catalog.countries()) {
      Set<String> regionCodes = new HashSet<>();
      for (CatalogRegion region : safe(country.regions())) {
        regionCodes.add(region.regionCode());
      }
      for (CatalogTenancyRule rule : safe(country.tenancyRules())) {
        assertThat(rule.topic()).as("topic in %s", country.countryCode()).isNotNull();
        assertThat(rule.label()).as("label in %s", country.countryCode()).isNotBlank();
        assertThat(rule.value()).as("value in %s", country.countryCode()).isNotBlank();
        if (rule.regionCode() != null) {
          assertThat(regionCodes)
              .as(
                  "tenancy rule in %s references region '%s' that is not declared",
                  country.countryCode(), rule.regionCode())
              .contains(rule.regionCode());
        }
        if (rule.effectiveFrom() != null) {
          assertThat(rule.effectiveFrom())
              .as("effectiveFrom in %s", country.countryCode())
              .matches("\\d{4}-\\d{2}-\\d{2}");
        }
      }
    }
  }

  @Test
  @DisplayName("Greece: free residential rents, 3-year minimum term, 2026 commercial 3% cap")
  void greece_recordsKeyFacts() {
    CatalogCountry greece = country("GR");

    assertThat(greece.countryName()).isEqualTo("Greece");
    assertThat(greece.hasRegionalRegulations()).isFalse();
    assertThat(greece.lateFee().policy()).isEqualTo(LateFeePolicy.ALLOWED);
    // Code of Civil Procedure art. 637: bailiff-served demand 15 days before the application.
    assertThat(greece.formalNoticeDays()).isEqualTo(15);

    // ν. 5007/2022 art. 96 par. 1 as amended by ν. 5255/2025 art. 59: commercial leases only.
    assertThat(safe(greece.rules()))
        .anySatisfy(
            rule -> {
              assertThat(rule.year()).isEqualTo(2026);
              assertThat(rule.propertyType()).isEqualTo("COMMERCIAL");
              assertThat(rule.maxIncreaseType()).isEqualTo(MaxIncreaseType.STATUTORY_CAP);
              assertThat(rule.maxIncreasePercentage()).isEqualByComparingTo("3");
            });
    assertThat(safe(greece.rules()))
        .filteredOn(rule -> "RESIDENTIAL".equals(rule.propertyType()))
        .isNotEmpty()
        .allSatisfy(
            rule -> {
              assertThat(rule.maxIncreaseType()).isEqualTo(MaxIncreaseType.NEGOTIATED);
              assertThat(rule.maxIncreasePercentage()).isNull();
            });

    // ν. 1703/1987 art. 2 par. 1 as replaced by ν. 2235/1994 art. 1 par. 5.
    assertThat(safe(greece.tenancyRules()))
        .anySatisfy(
            rule -> {
              assertThat(rule.topic()).isEqualTo(TenancyRuleTopic.TENANCY_DURATION);
              // ν. 1703/1987 art. 1 par. 1 limits the law to the tenant's main residence; the
              // scope must be visible, since the page does not render notes.
              assertThat(rule.label()).containsIgnoringCase("main residence");
              assertThat(rule.value()).isEqualTo("3 years");
              assertThat(rule.legalBasis()).contains("1703/1987").contains("2235/1994");
            });
    assertThat(safe(greece.tenancyRules()))
        .extracting(CatalogTenancyRule::topic)
        .contains(
            TenancyRuleTopic.TENANCY_DURATION,
            TenancyRuleTopic.DEPOSIT,
            TenancyRuleTopic.REGISTRATION,
            TenancyRuleTopic.NOTICE_PERIOD);
    assertThat(greece.summary()).containsIgnoringCase("main residence");

    // The 2025 cap was set by ν. 5164/2024 art. 75 (ΦΕΚ Α' 202/12.12.2024), not by the 2026
    // extension.
    assertThat(safe(greece.rules()))
        .filteredOn(rule -> rule.year() == 2025 && "COMMERCIAL".equals(rule.propertyType()))
        .singleElement()
        .satisfies(
            rule -> {
              assertThat(rule.notes()).contains("5164/2024");
              assertThat(rule.sourceUrl()).contains("2024/5164");
            });
  }

  @Test
  @DisplayName("Greece: the debated deposit cap carries its qualifier in the visible value")
  void greece_depositValueIsQualified() {
    // The regulations page renders label/value/legalBasis/source but never notes, so the
    // "continued force debated" caveat on ν. 1703/1987 art. 2 par. 2 must sit in the value.
    assertThat(safe(country("GR").tenancyRules()))
        .filteredOn(rule -> rule.topic() == TenancyRuleTopic.DEPOSIT)
        .singleElement()
        .satisfies(
            rule -> {
              assertThat(rule.value()).contains("2 months' rent");
              assertThat(rule.value()).containsIgnoringCase("debated");
              assertThat(rule.value()).containsIgnoringCase("verify");
            });
  }

  @Test
  @DisplayName("Greece: every rule and tenancy fact has a legal basis and an https source")
  void greece_everyFactIsSourced() {
    CatalogCountry greece = country("GR");

    assertThat(greece.lastReviewedAt()).startsWith("2026-10-04");
    assertThat(greece.summary()).isNotBlank();
    assertThat(greece.lateFee().notes()).isNotBlank();
    assertThat(safe(greece.rules()))
        .isNotEmpty()
        .allSatisfy(
            rule -> {
              assertThat(rule.sourceUrl()).startsWith("https://");
              assertThat(rule.notes()).isNotBlank();
              assertThat(rule.effectiveDate()).matches("\\d{4}-\\d{2}-\\d{2}");
            });
    assertThat(safe(greece.tenancyRules()))
        .isNotEmpty()
        .allSatisfy(
            rule -> {
              assertThat(rule.legalBasis()).as("legalBasis of '%s'", rule.label()).isNotBlank();
              assertThat(rule.sourceUrl())
                  .as("sourceUrl of '%s'", rule.label())
                  .startsWith("https://");
              assertThat(rule.notes()).as("notes of '%s'", rule.label()).isNotBlank();
              assertThat(rule.regionCode()).as("GR has no regions").isNull();
            });
  }

  @Test
  @DisplayName("a country with no tenancy rules loads cleanly")
  void tenancyRules_areOptional() {
    RentRegulationCatalog catalog = loader.load();

    assertThat(catalog.countries())
        .as("the bundled catalog ships most countries without tenancy rules")
        .anySatisfy(c -> assertThat(safe(c.tenancyRules())).isEmpty());
  }

  @Test
  @DisplayName("an unknown tenancy-rule topic fails the parse")
  void unknownTopic_failsParse() {
    String json =
        """
        {"version":"t","generatedAt":"2026-01-01","countries":[
          {"countryCode":"XX","countryName":"X","hasRegionalRegulations":false,
           "tenancyRules":[{"topic":"NOT_A_TOPIC","label":"l","value":"v"}]}]}
        """;

    assertThatThrownBy(() -> new ObjectMapper().readValue(json, RentRegulationCatalog.class))
        .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
  }

  private CatalogCountry country(String code) {
    return loader.load().countries().stream()
        .filter(c -> code.equals(c.countryCode()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("catalog has no country " + code));
  }

  private static <T> List<T> safe(List<T> list) {
    return list == null ? List.of() : list;
  }
}
