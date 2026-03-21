package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.WwsCalculationRequest;
import com.buurman.dto.response.WwsCalculationResponse;
import com.buurman.dto.response.WwsCategoryBreakdown;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyResidentialDetailsRepository;
import com.buurman.repository.WwsCalculationRepository;
import com.buurman.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("WwsPointsCalculatorService")
class WwsPointsCalculatorServiceTest {

  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private PropertyResidentialDetailsRepository residentialDetailsRepository;
  @Mock private PropertyOutdoorAreaRepository outdoorAreaRepository;
  @Mock private WwsCalculationRepository wwsCalculationRepository;

  private WwsPointsCalculatorService service;
  private UserPrincipal principal;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of("UTC"));
  private static final PropertyIdentifier PROP_ID = PropertyIdentifier.of("prop_test123456789012345");

  @BeforeEach
  void setUp() {
    service =
        new WwsPointsCalculatorService(
            FIXED_CLOCK,
            contractRepository,
            propertyRepository,
            residentialDetailsRepository,
            outdoorAreaRepository,
            wwsCalculationRepository,
            new ObjectMapper());
    principal =
        new UserPrincipal(
            java.util.UUID.randomUUID(),
            "usr_test",
            "kc-123",
            "test@example.com",
            "Test User",
            java.util.UUID.randomUUID(),
            "team_test",
            com.buurman.domain.TeamRole.TEAM_ADMIN);
  }

  private WwsCalculationRequest simpleRequest(String version) {
    return new WwsCalculationRequest(
        version,
        PROP_ID,
        Optional.empty(),
        Optional.of(new BigDecimal("75")),
        Optional.of(3),
        Optional.of(3),
        Optional.of("A"),
        Optional.of(new BigDecimal("7")),
        Optional.of(new BigDecimal("5")),
        Optional.of(new BigDecimal("200000")),
        Optional.of(new BigDecimal("20")),
        Optional.of("GARAGE"),
        Optional.of(1),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @Nested
  @DisplayName("Basic Calculation")
  class BasicCalculation {

    @Test
    @DisplayName("calculates total points for a simple 2025 property")
    void calculatesPointsForSimpleProperty() {
      WwsCalculationResponse response = service.calculate(simpleRequest("2025"), principal);

      assertThat(response.totalPoints()).isPositive();
      assertThat(response.sectorClassification()).isNotBlank();
      assertThat(response.systemVersion()).isEqualTo("2025");
      assertThat(response.breakdown()).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("surface area yields 1 point per m2")
    void surfaceAreaOnePointPerSqm() {
      WwsCalculationResponse response = service.calculate(simpleRequest("2025"), principal);

      WwsCategoryBreakdown surfaceArea =
          response.breakdown().stream()
              .filter(b -> "SURFACE_AREA".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(surfaceArea.points()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("rooms yield 2 points each")
    void roomsTwoPointsEach() {
      WwsCalculationResponse response = service.calculate(simpleRequest("2025"), principal);

      WwsCategoryBreakdown rooms =
          response.breakdown().stream()
              .filter(b -> "ROOMS".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(rooms.points()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("throws on unsupported system version")
    void throwsOnUnsupportedVersion() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2020",
              PROP_ID,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      assertThatThrownBy(() -> service.calculate(request, principal))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported WWS system version");
    }
  }

  @Nested
  @DisplayName("Energy Label Points")
  class EnergyLabelPoints {

    @Test
    @DisplayName("energy label A yields 41 pts for 2025 version")
    void energyLabelAPost2024() {
      WwsCalculationResponse response = service.calculate(simpleRequest("2025"), principal);

      WwsCategoryBreakdown energy =
          response.breakdown().stream()
              .filter(b -> "ENERGY_LABEL".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(energy.points()).isEqualByComparingTo("41");
    }

    @Test
    @DisplayName("energy label A yields 32 pts for 2023 version")
    void energyLabelAPre2024() {
      WwsCalculationResponse response = service.calculate(simpleRequest("2023"), principal);

      WwsCategoryBreakdown energy =
          response.breakdown().stream()
              .filter(b -> "ENERGY_LABEL".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(energy.points()).isEqualByComparingTo("32");
    }

    @Test
    @DisplayName("energy label G yields negative points for 2025 version")
    void energyLabelGNegativePost2024() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              Optional.empty(),
              Optional.of(new BigDecimal("50")),
              Optional.empty(),
              Optional.empty(),
              Optional.of("G"),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown energy =
          response.breakdown().stream()
              .filter(b -> "ENERGY_LABEL".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(energy.points()).isEqualByComparingTo("-15");
    }
  }

  @Nested
  @DisplayName("Classification")
  class Classification {

    @Test
    @DisplayName("REGULATED classification for low points (2025)")
    void regulatedForLowPoints() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              Optional.empty(),
              Optional.of(new BigDecimal("30")),
              Optional.of(2),
              Optional.of(2),
              Optional.of("C"),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.of(new BigDecimal("5")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      assertThat(response.sectorClassification()).isEqualTo("REGULATED");
      assertThat(response.maxRentIndication()).isPresent();
    }

    @Test
    @DisplayName("FREE_SECTOR has no max rent indication")
    void freeSectorNoMaxRent() {
      // High points property to get free sector
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              Optional.empty(),
              Optional.of(new BigDecimal("200")),
              Optional.of(8),
              Optional.of(8),
              Optional.of("A++++"),
              Optional.of(new BigDecimal("20")),
              Optional.of(new BigDecimal("15")),
              Optional.of(new BigDecimal("500000")),
              Optional.of(new BigDecimal("50")),
              Optional.of("GARAGE"),
              Optional.of(2),
              Optional.empty(),
              Optional.empty(),
              Optional.of(3),
              Optional.of(new BigDecimal("30")));

      WwsCalculationResponse response = service.calculate(request, principal);

      assertThat(response.sectorClassification()).isEqualTo("FREE_SECTOR");
      assertThat(response.maxRentIndication()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Outdoor Space Formula")
  class OutdoorSpace {

    @Test
    @DisplayName("2025: no outdoor space yields -5 pts")
    void noOutdoorSpacePost2024() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              Optional.empty(),
              Optional.of(new BigDecimal("50")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown outdoor =
          response.breakdown().stream()
              .filter(b -> "OUTDOOR_SPACE".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(outdoor.points()).isEqualByComparingTo("-5");
    }

    @Test
    @DisplayName("2023: no outdoor space yields 0 pts")
    void noOutdoorSpacePre2024() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2023",
              PROP_ID,
              Optional.empty(),
              Optional.of(new BigDecimal("50")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown outdoor =
          response.breakdown().stream()
              .filter(b -> "OUTDOOR_SPACE".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(outdoor.points()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("2025: outdoor space capped at 15 pts")
    void outdoorSpaceCappedAt15() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.of(new BigDecimal("200")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown outdoor =
          response.breakdown().stream()
              .filter(b -> "OUTDOOR_SPACE".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(outdoor.points()).isEqualByComparingTo("15");
    }
  }

  @Nested
  @DisplayName("Renovation Points")
  class RenovationPoints {

    @Test
    @DisplayName("2023: renovation not applicable")
    void renovationNotApplicablePre2024() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2023",
              PROP_ID,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.of(new BigDecimal("10000")),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown renovation =
          response.breakdown().stream()
              .filter(b -> "RENOVATION".equals(b.key()))
              .findFirst()
              .orElseThrow();
      assertThat(renovation.points()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("2025: renovation investment / 332 yields points")
    void renovationPost2024() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.of(new BigDecimal("10000")),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown renovation =
          response.breakdown().stream()
              .filter(b -> "RENOVATION".equals(b.key()))
              .findFirst()
              .orElseThrow();
      // 10000 / 332 = 30.12
      assertThat(renovation.points()).isEqualByComparingTo("30.12");
    }
  }
}
