package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationTenancyRule;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.util.EntityPrefix;
import com.buurman.util.SidGenerator;

@DisplayName("RentRegulationRepository Integration")
class RentRegulationRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private RentRegulationRepository repository;

  @BeforeEach
  void setUp() {
    repository = new RentRegulationRepository(dsl, CLOCK);
  }

  @Test
  @DisplayName("a region-scoped tenancy rule round-trips and does not block reference wipe")
  void tenancyRule_roundTripsAndDeletes() {
    RentRegulationCountry country =
        repository.saveCountry(
            RentRegulationCountry.builder()
                .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRC)))
                .countryCode("XT")
                .countryName("Testland")
                .hasRegionalRegulations(true)
                .lateFeePolicy(LateFeePolicy.FORBIDDEN)
                .build());
    RentRegulationRegion region =
        repository.saveRegion(
            RentRegulationRegion.builder()
                .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRG)))
                .countryId(country.getId())
                .regionCode("XT-1")
                .regionName("Region One")
                .build());

    repository.saveTenancyRule(
        RentRegulationTenancyRule.builder()
            .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRT)))
            .countryId(country.getId())
            .regionId(Optional.of(region.getId()))
            .topic(TenancyRuleTopic.TENANCY_DURATION)
            .label("Minimum fixed term")
            .value("5 years")
            .effectiveFrom(Optional.of(LocalDate.parse("2026-01-01")))
            .legalBasis(Optional.of("MRG § 29"))
            .build());

    List<RentRegulationTenancyRule> found = repository.findTenancyRulesByCountryId(country.getId());
    assertThat(found).hasSize(1);
    assertThat(found.getFirst().getTopic()).isEqualTo(TenancyRuleTopic.TENANCY_DURATION);
    assertThat(found.getFirst().getValue()).isEqualTo("5 years");
    assertThat(found.getFirst().getRegionId()).contains(region.getId());
    assertThat(found.getFirst().getEffectiveFrom()).contains(LocalDate.parse("2026-01-01"));

    // The FK to regions means the wipe must delete tenancy rules FIRST or this throws.
    repository.deleteAllReferenceData();
    assertThat(repository.findAllTenancyRules()).isEmpty();
  }
}
