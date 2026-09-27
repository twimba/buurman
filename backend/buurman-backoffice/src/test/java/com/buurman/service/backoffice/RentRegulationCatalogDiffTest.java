package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.domain.regulation.CatalogCountry;
import com.buurman.domain.regulation.CatalogLateFee;
import com.buurman.domain.regulation.CatalogRule;
import com.buurman.domain.regulation.CatalogTenancyRule;
import com.buurman.domain.regulation.RentRegulationCatalog;
import com.buurman.dto.response.RentRegulationCatalogDiff;
import com.buurman.dto.response.RentRegulationCountryDiff;
import com.buurman.dto.response.RentRegulationDiffEntry;
import com.buurman.repository.RentRegulationRepository;

@DisplayName("RentRegulationCatalogService.diff")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class RentRegulationCatalogDiffTest {

  @Mock private RentRegulationRepository repository;
  @Mock private RentRegulationCatalogLoader loader;

  private RentRegulationCatalogService service;

  private static final UUID NL_ID = UUID.randomUUID();
  private static final UUID XX_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new RentRegulationCatalogService(
            repository, loader, Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("reports added / removed countries and added / removed / changed rules")
  void diff_reportsAllChangeTypes() {
    // Bundled catalog (target): NL (rule A unchanged-key but new note, rule B new) + FR (new
    // country)
    RentRegulationCatalog target =
        new RentRegulationCatalog(
            "2026.2",
            "2026-06-20",
            "desc",
            List.of(
                country(
                    "NL",
                    "Netherlands",
                    List.of(
                        catalogRule(2026, "REGULATED", new BigDecimal("4.10"), "inflation-based"),
                        catalogRule(2026, "FREE_SECTOR", new BigDecimal("4.40"), "cpi"))),
                country(
                    "FR",
                    "France",
                    List.of(catalogRule(2026, "ALL", new BigDecimal("0.78"), null)))));
    when(loader.load()).thenReturn(target);

    // Current DB: NL (rule A same key but old note, rule C only-in-db) + XX (country only-in-db)
    when(repository.findAllCountries())
        .thenReturn(
            List.of(
                domainCountry(NL_ID, "NL", "Netherlands"), domainCountry(XX_ID, "XX", "Atlantis")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules())
        .thenReturn(
            List.of(
                domainRule(NL_ID, 2026, "REGULATED", new BigDecimal("4.10"), "wage-growth"),
                domainRule(NL_ID, 2026, "OLD_TIER", new BigDecimal("9.99"), "gone")));
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    // Countries: FR added, XX removed, NL changed
    assertThat(diff.countries().added()).isEqualTo(1);
    assertThat(diff.countries().removed()).isEqualTo(1);
    assertThat(diff.countries().changed()).isEqualTo(1);

    // Rules: FR's rule (added) + NL FREE_SECTOR (added) = 2 added; NL OLD_TIER removed = 1; NL
    // REGULATED note changed = 1
    assertThat(diff.rules().added()).isEqualTo(2);
    assertThat(diff.rules().removed()).isEqualTo(1);
    assertThat(diff.rules().changed()).isEqualTo(1);

    RentRegulationCountryDiff nl =
        diff.byCountry().stream()
            .filter(c -> c.countryCode().equals("NL"))
            .findFirst()
            .orElseThrow();
    assertThat(nl.status()).isEqualTo("MODIFIED");

    RentRegulationDiffEntry changed =
        nl.changes().stream().filter(e -> e.op().equals("CHANGED")).findFirst().orElseThrow();
    assertThat(changed.entity()).isEqualTo("RULE");
    assertThat(changed.fields())
        .anySatisfy(
            f -> {
              assertThat(f.field()).isEqualTo("notes");
              assertThat(f.before()).isEqualTo("wage-growth");
              assertThat(f.after()).isEqualTo("inflation-based");
            });
  }

  @Test
  @DisplayName("identical catalog and database yield no changes")
  void diff_identical_yieldsNoChanges() {
    RentRegulationCatalog target =
        new RentRegulationCatalog(
            "2026.2",
            "2026-06-20",
            "desc",
            List.of(
                country(
                    "NL",
                    "Netherlands",
                    List.of(catalogRule(2026, "REGULATED", new BigDecimal("4.10"), "same")))));
    when(loader.load()).thenReturn(target);
    when(repository.findAllCountries())
        .thenReturn(List.of(domainCountry(NL_ID, "NL", "Netherlands")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules())
        .thenReturn(List.of(domainRule(NL_ID, 2026, "REGULATED", new BigDecimal("4.10"), "same")));
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    assertThat(diff.countries().added() + diff.countries().removed() + diff.countries().changed())
        .isZero();
    assertThat(diff.rules().added() + diff.rules().removed() + diff.rules().changed()).isZero();
    assertThat(diff.byCountry()).isEmpty();
  }

  @Test
  @DisplayName("decimal scale differences (2.00 vs 2.0) are not reported as changes")
  void diff_ignoresDecimalScaleOnlyDifferences() {
    RentRegulationCatalog target =
        new RentRegulationCatalog(
            "2026.2",
            "2026-06-20",
            "desc",
            List.of(
                country(
                    "PT",
                    "Portugal",
                    List.of(catalogRule(2023, "ALL", new BigDecimal("2.0"), "same")))));
    when(loader.load()).thenReturn(target);
    when(repository.findAllCountries()).thenReturn(List.of(domainCountry(NL_ID, "PT", "Portugal")));
    when(repository.findAllRegions()).thenReturn(List.of());
    // DB column is DECIMAL(5,2) → value comes back as 2.00 (scale 2)
    when(repository.findAllRules())
        .thenReturn(List.of(domainRule(NL_ID, 2023, "ALL", new BigDecimal("2.00"), "same")));
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    assertThat(diff.rules().changed()).isZero();
    assertThat(diff.byCountry()).isEmpty();
  }

  @Test
  @DisplayName("a late-fee regime change in the catalogue is reported as a country change")
  void diff_reportsLateFeeChanges() {
    CatalogCountry pt =
        new CatalogCountry(
            "PT",
            "Portugal",
            false,
            null,
            null,
            null,
            List.of(),
            new CatalogLateFee(LateFeePolicy.CAPPED, new BigDecimal("20.0"), "Art. 1041 CC"),
            8,
            null);
    RentRegulationCatalog target =
        new RentRegulationCatalog("2026.2", "2026-06-20", "desc", List.of(pt));
    when(loader.load()).thenReturn(target);
    when(repository.findAllCountries()).thenReturn(List.of(domainCountry(NL_ID, "PT", "Portugal")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules()).thenReturn(List.of());
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    assertThat(diff.countries().changed()).isEqualTo(1);
    RentRegulationDiffEntry entry =
        diff.byCountry().getFirst().changes().stream()
            .filter(e -> e.entity().equals("COUNTRY"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.fields())
        .anySatisfy(
            f -> {
              assertThat(f.field()).isEqualTo("lateFeePolicy");
              assertThat(f.before()).isEmpty();
              assertThat(f.after()).isEqualTo("CAPPED");
            })
        .anySatisfy(
            f -> {
              assertThat(f.field()).isEqualTo("lateFeeMaxPercentage");
              assertThat(f.after()).isEqualTo("20.0");
            });
  }

  @Test
  @DisplayName("a tenancy rule the catalogue adds is reported as a country change")
  void diff_detectsAddedTenancyRule() {
    CatalogCountry at =
        new CatalogCountry(
            "AT",
            "Austria",
            false,
            null,
            null,
            null,
            List.of(),
            null,
            14,
            List.of(
                new CatalogTenancyRule(
                    TenancyRuleTopic.TENANCY_DURATION,
                    null,
                    "Minimum fixed term",
                    "5 years",
                    "2026-01-01",
                    "MRG § 29",
                    "https://ris.bka.gv.at",
                    null)));
    when(loader.load())
        .thenReturn(new RentRegulationCatalog("2026.6", "2026-09-26", "desc", List.of(at)));
    when(repository.findAllCountries()).thenReturn(List.of(domainCountry(NL_ID, "AT", "Austria")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules()).thenReturn(List.of());
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    RentRegulationDiffEntry entry =
        diff.byCountry().getFirst().changes().stream()
            .filter(e -> e.entity().equals("COUNTRY"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.fields())
        .anySatisfy(
            f -> {
              assertThat(f.field()).isEqualTo("tenancyRule[TENANCY_DURATION/Minimum fixed term]");
              assertThat(f.before()).isEmpty();
              assertThat(f.after()).contains("5 years");
            });
  }

  @Test
  @DisplayName("a country with no tenancy rules on either side reports no tenancy diff")
  void diff_noTenancyRules_yieldsNoTenancyField() {
    CatalogCountry at =
        new CatalogCountry(
            "AT", "Austria", false, null, null, null, List.of(), null, 14, List.of());
    when(loader.load())
        .thenReturn(new RentRegulationCatalog("2026.6", "2026-09-26", "desc", List.of(at)));
    when(repository.findAllCountries()).thenReturn(List.of(domainCountry(NL_ID, "AT", "Austria")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules()).thenReturn(List.of());
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    assertThat(diff.byCountry().getFirst().changes())
        .allSatisfy(
            e ->
                assertThat(e.fields())
                    .noneSatisfy(f -> assertThat(f.field()).startsWith("tenancyRule[")));
  }

  // ---- builders ----

  private static CatalogCountry country(String code, String name, List<CatalogRule> rules) {
    return new CatalogCountry(code, name, false, null, null, null, rules, null, null, null);
  }

  private static CatalogRule catalogRule(int year, String category, BigDecimal pct, String notes) {
    return new CatalogRule(
        null,
        year,
        category,
        pct,
        MaxIncreaseType.FIXED_PERCENTAGE,
        null,
        null,
        null,
        null,
        RentFrequency.ANNUAL,
        null,
        null,
        notes,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static RentRegulationCountry domainCountry(UUID id, String code, String name) {
    return RentRegulationCountry.builder()
        .id(id)
        .identifier(Optional.empty())
        .countryCode(code)
        .countryName(name)
        .hasRegionalRegulations(false)
        .build();
  }

  private static RentRegulationRule domainRule(
      UUID countryId, int year, String category, BigDecimal pct, String notes) {
    return RentRegulationRule.builder()
        .countryId(countryId)
        .year(year)
        .propertyCategory(category)
        .maxIncreasePercentage(Optional.of(pct))
        .maxIncreaseType(MaxIncreaseType.FIXED_PERCENTAGE)
        .frequency(RentFrequency.ANNUAL)
        .notes(Optional.ofNullable(notes))
        .build();
  }
}
