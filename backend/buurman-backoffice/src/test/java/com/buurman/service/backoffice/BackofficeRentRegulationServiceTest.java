package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationTenancyRule;
import com.buurman.domain.Sid;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.dto.response.RentRegulationTenancyRuleResponse;
import com.buurman.mapper.RentRegulationMapper;
import com.buurman.repository.RentRegulationRepository;

@DisplayName("BackofficeRentRegulationService.listTenancyRules")
@ExtendWith(MockitoExtension.class)
class BackofficeRentRegulationServiceTest {

  private static final UUID COUNTRY_ID = UUID.randomUUID();
  private static final UUID REGION_ID = UUID.randomUUID();

  @Mock private RentRegulationRepository repository;

  private BackofficeRentRegulationService service;

  @BeforeEach
  void setUp() {
    service = new BackofficeRentRegulationService(repository, new RentRegulationMapper());
    when(repository.getCountryByCode("GR"))
        .thenReturn(RentRegulationCountry.builder().id(COUNTRY_ID).countryCode("GR").build());
  }

  @Test
  @DisplayName("returns every field, notes included, and resolves the region code")
  void listTenancyRules_mapsAllFields() {
    RentRegulationTenancyRule national =
        RentRegulationTenancyRule.builder()
            .identifier(Optional.of(Sid.of("RTRGR0000000000000000000001")))
            .countryId(COUNTRY_ID)
            .topic(TenancyRuleTopic.DEPOSIT)
            .label("Security deposit")
            .value("At most 2 months' rent (debated - verify with counsel)")
            .effectiveFrom(Optional.of(LocalDate.of(1987, 5, 27)))
            .legalBasis(Optional.of("ν. 1703/1987 art. 2 par. 2"))
            .sourceUrl(Optional.of("https://api.et.gr/apiLAW/1/1987/1703/pdf"))
            .notes(Optional.of("continued force debated"))
            .build();
    RentRegulationTenancyRule regional =
        RentRegulationTenancyRule.builder()
            .identifier(Optional.of(Sid.of("RTRGR0000000000000000000002")))
            .countryId(COUNTRY_ID)
            .regionId(Optional.of(REGION_ID))
            .topic(TenancyRuleTopic.OTHER)
            .label("Regional fact")
            .value("value")
            .build();
    when(repository.findTenancyRulesByCountryId(COUNTRY_ID))
        .thenReturn(List.of(national, regional));
    when(repository.findRegionsByCountryId(COUNTRY_ID))
        .thenReturn(
            List.of(
                RentRegulationRegion.builder()
                    .id(REGION_ID)
                    .countryId(COUNTRY_ID)
                    .regionCode("ATT")
                    .regionName("Attica")
                    .build()));

    List<RentRegulationTenancyRuleResponse> rules = service.listTenancyRules("GR");

    assertThat(rules).hasSize(2);
    RentRegulationTenancyRuleResponse deposit = rules.getFirst();
    assertThat(deposit.topic()).isEqualTo(TenancyRuleTopic.DEPOSIT);
    assertThat(deposit.label()).isEqualTo("Security deposit");
    assertThat(deposit.value()).contains("debated");
    assertThat(deposit.effectiveFrom()).contains(LocalDate.of(1987, 5, 27));
    assertThat(deposit.legalBasis()).contains("ν. 1703/1987 art. 2 par. 2");
    assertThat(deposit.sourceUrl()).contains("https://api.et.gr/apiLAW/1/1987/1703/pdf");
    assertThat(deposit.notes()).contains("continued force debated");
    assertThat(deposit.regionCode()).isEmpty();
    assertThat(rules.get(1).regionCode()).contains("ATT");
  }

  @Test
  @DisplayName("a country without tenancy rules yields an empty list")
  void listTenancyRules_empty() {
    when(repository.findTenancyRulesByCountryId(COUNTRY_ID)).thenReturn(List.of());
    when(repository.findRegionsByCountryId(COUNTRY_ID)).thenReturn(List.of());

    assertThat(service.listTenancyRules("GR")).isEmpty();
  }
}
