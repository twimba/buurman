package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationTenancyRule;
import com.buurman.domain.TeamRole;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.dto.response.RentRegulationCountryDetailResponse;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.SidGenerator;

@ExtendWith(MockitoExtension.class)
class RentRegulationServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID COUNTRY_ID = UUID.randomUUID();
  private static final UUID REGION_ID = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

  @Mock private RentRegulationRepository rentRegulationRepository;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private RentRegulationService service;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    service = new RentRegulationService(rentRegulationRepository, clock);
    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "t@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);
  }

  private static RentRegulationCountry country() {
    return RentRegulationCountry.builder()
        .id(COUNTRY_ID)
        .identifier(Optional.of(SidGenerator.newRentRegulationCountryId()))
        .countryCode("NL")
        .countryName("Netherlands")
        .hasRegionalRegulations(true)
        .lateFeePolicy(LateFeePolicy.FORBIDDEN)
        .build();
  }

  private static RentRegulationRegion region() {
    return RentRegulationRegion.builder()
        .id(REGION_ID)
        .identifier(Optional.of(SidGenerator.newRentRegulationRegionId()))
        .countryId(COUNTRY_ID)
        .regionCode("NH")
        .regionName("Noord-Holland")
        .build();
  }

  private static RentRegulationTenancyRule tenancyRule(Optional<UUID> regionId) {
    return RentRegulationTenancyRule.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(SidGenerator.newRentRegulationTenancyRuleId()))
        .countryId(COUNTRY_ID)
        .regionId(regionId)
        .topic(TenancyRuleTopic.TENANCY_DURATION)
        .label("Minimum fixed term")
        .value("5 years")
        .build();
  }

  private void stubCountryAndEmptyExtras() {
    when(rentRegulationRepository.findCountryByCode("NL")).thenReturn(Optional.of(country()));
    when(rentRegulationRepository.findRegionsByCountryId(COUNTRY_ID)).thenReturn(List.of(region()));
    when(rentRegulationRepository.findRulesByCountryId(COUNTRY_ID)).thenReturn(List.of());
  }

  @Nested
  @DisplayName("getCountryDetail — tenancy rule regionId -> regionCode resolution")
  class TenancyRuleRegionResolution {

    @Test
    @DisplayName("a tenancy rule whose regionId matches a loaded region resolves to its regionCode")
    void resolvesKnownRegion() {
      stubCountryAndEmptyExtras();
      when(rentRegulationRepository.findTenancyRulesByCountryId(COUNTRY_ID))
          .thenReturn(List.of(tenancyRule(Optional.of(REGION_ID))));

      RentRegulationCountryDetailResponse response = service.getCountryDetail("NL", principal);

      assertThat(response.tenancyRules()).hasSize(1);
      assertThat(response.tenancyRules().get(0).regionCode()).contains("NH");
    }

    @Test
    @DisplayName("a national rule (regionId empty) yields an empty regionCode")
    void nationalRuleHasNoRegionCode() {
      stubCountryAndEmptyExtras();
      when(rentRegulationRepository.findTenancyRulesByCountryId(COUNTRY_ID))
          .thenReturn(List.of(tenancyRule(Optional.empty())));

      RentRegulationCountryDetailResponse response = service.getCountryDetail("NL", principal);

      assertThat(response.tenancyRules()).hasSize(1);
      assertThat(response.tenancyRules().get(0).regionCode()).isEmpty();
    }

    @Test
    @DisplayName("a regionId matching no loaded region degrades to empty rather than throwing")
    void orphanedRegionIdDegradesGracefully() {
      UUID orphanedRegionId = UUID.randomUUID();
      stubCountryAndEmptyExtras();
      when(rentRegulationRepository.findTenancyRulesByCountryId(COUNTRY_ID))
          .thenReturn(List.of(tenancyRule(Optional.of(orphanedRegionId))));

      RentRegulationCountryDetailResponse response = service.getCountryDetail("NL", principal);

      assertThat(response.tenancyRules()).hasSize(1);
      assertThat(response.tenancyRules().get(0).regionCode()).isEmpty();
    }
  }
}
