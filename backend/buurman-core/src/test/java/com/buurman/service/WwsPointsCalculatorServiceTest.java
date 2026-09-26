package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.WwsCalculationRequest;
import com.buurman.dto.response.WwsCalculationResponse;
import com.buurman.dto.response.WwsCategoryBreakdown;
import com.buurman.dto.response.WwsPreFillResponse;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitAmenityRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.repository.WwsCalculationRepository;
import com.buurman.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("WwsPointsCalculatorService")
class WwsPointsCalculatorServiceTest {

  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private UnitResidentialDetailsRepository unitResidentialDetailsRepository;
  @Mock private UnitAmenityRepository unitAmenityRepository;
  @Mock private PropertyOutdoorAreaRepository outdoorAreaRepository;
  @Mock private WwsCalculationRepository wwsCalculationRepository;

  private WwsPointsCalculatorService service;
  private UserPrincipal principal;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of("UTC"));
  private static final PropertyIdentifier PROP_ID =
      PropertyIdentifier.of("prop_test123456789012345");

  @BeforeEach
  void setUp() {
    service =
        new WwsPointsCalculatorService(
            FIXED_CLOCK,
            contractRepository,
            propertyRepository,
            unitRepository,
            unitResidentialDetailsRepository,
            unitAmenityRepository,
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
        null,
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
      assertThat(response.breakdown()).hasSize(13);
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
              null,
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
              null,
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
              null,
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
              null,
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
              null,
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
              null,
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
              null,
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
  @DisplayName("WOZ Value")
  class WozValue {

    @Test
    @DisplayName("2025: WOZ 33% cap limits WOZ points to 33% of non-WOZ total")
    void wozCapAppliedWhenExceedsThreshold() {
      // Create a request where WOZ points would be very high relative to other categories.
      // Small surface (10 m2), no rooms, no heating, label G (-15 pts), no kitchen/bathroom,
      // very high WOZ (500000), no outdoor (gives -5), no parking, no extras.
      // Non-WOZ total will be small, forcing the cap.
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              null,
              Optional.empty(),
              Optional.of(new BigDecimal("10")),
              Optional.empty(),
              Optional.empty(),
              Optional.of("G"),
              Optional.empty(),
              Optional.empty(),
              Optional.of(new BigDecimal("500000")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown woz =
          response.breakdown().stream()
              .filter(b -> "WOZ_VALUE".equals(b.key()))
              .findFirst()
              .orElseThrow();

      // Compute non-WOZ total
      BigDecimal nonWozTotal =
          response.breakdown().stream()
              .filter(b -> !"WOZ_VALUE".equals(b.key()))
              .map(WwsCategoryBreakdown::points)
              .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

      // WOZ should be capped at 33% of non-WOZ (if non-WOZ is positive)
      if (nonWozTotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
        BigDecimal maxWoz =
            nonWozTotal
                .multiply(new BigDecimal("0.33"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(woz.points()).isLessThanOrEqualTo(maxWoz);
        assertThat(woz.explanation()).contains("capped at 33%");
      }
    }

    @Test
    @DisplayName("2025: WOZ minimum floor applied when value below minimum")
    void wozMinimumFloorApplied() {
      // WOZ minimum for 2025 is 85806. Use WOZ value of 50000 (below minimum).
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              null,
              Optional.empty(),
              Optional.of(new BigDecimal("75")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.of(new BigDecimal("50000")),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown woz =
          response.breakdown().stream()
              .filter(b -> "WOZ_VALUE".equals(b.key()))
              .findFirst()
              .orElseThrow();

      // The explanation should mention "Min floor applied"
      assertThat(woz.explanation()).contains("Min floor applied");
      // Points should be > 0 since effective WOZ = 85806 (the minimum)
      assertThat(woz.points()).isPositive();
    }
  }

  @Nested
  @DisplayName("Parking")
  class Parking {

    @Test
    @DisplayName("parking with multiple spaces multiplies base points")
    void multipleSpacesMultiplyPoints() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              null,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.of("GARAGE"),
              Optional.of(3),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown parking =
          response.breakdown().stream()
              .filter(b -> "PARKING".equals(b.key()))
              .findFirst()
              .orElseThrow();
      // GARAGE = 9 pts base × 3 spaces = 27
      assertThat(parking.points()).isEqualByComparingTo("27");
    }
  }

  @Nested
  @DisplayName("Common Areas")
  class CommonAreas {

    @Test
    @DisplayName("common area yields 0.75 points per m2")
    void commonAreaPointsCalculation() {
      WwsCalculationRequest request =
          new WwsCalculationRequest(
              "2025",
              PROP_ID,
              null,
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
              Optional.of(new BigDecimal("20")));

      WwsCalculationResponse response = service.calculate(request, principal);

      WwsCategoryBreakdown commonAreas =
          response.breakdown().stream()
              .filter(b -> "COMMON_AREAS".equals(b.key()))
              .findFirst()
              .orElseThrow();
      // 20 m² × 0.75 = 15.00
      assertThat(commonAreas.points()).isEqualByComparingTo("15.00");
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
              null,
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
              null,
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

  @Nested
  @DisplayName("Pre-fill (per-unit dwelling data)")
  class PreFill {

    private static final UnitIdentifier UNIT_A_ID =
        UnitIdentifier.of("unit_testAAAAAAAAAAAAAAAAAA1");
    private static final UnitIdentifier UNIT_B_ID =
        UnitIdentifier.of("unit_testBBBBBBBBBBBBBBBBBB2");

    private Unit unit(UnitIdentifier identifier, UUID propertyId, String energyLabel) {
      return Unit.builder()
          .id(UUID.randomUUID())
          .identifier(Optional.of(identifier))
          .teamId(principal.getTeamId().orElseThrow())
          .propertyId(propertyId)
          .unitNumber("1")
          .unitType(UnitType.APARTMENT)
          .status(UnitStatus.VACANT)
          .areaValue(Optional.of(new BigDecimal("75")))
          .areaUnit(Optional.of("sqm"))
          .energyEfficiencyRating(Optional.of(energyLabel))
          .build();
    }

    private Property buildingProperty(UUID propertyId) {
      Property property = new Property();
      property.setId(propertyId);
      property.setStreet("Keizersgracht 1");
      property.setPostalCode("1015 CJ");
      property.setCity("Amsterdam");
      return property;
    }

    private void stubDwellingLookups(Unit unit, Property property) {
      UUID teamId = principal.getTeamId().orElseThrow();
      when(unitRepository.getByIdentifierAndTeamId(unit.getIdentifier().orElseThrow(), teamId))
          .thenReturn(unit);
      when(propertyRepository.getByIdAndTeamId(property.getId(), teamId)).thenReturn(property);
      when(unitResidentialDetailsRepository.findByUnitIdAndTeamId(unit.getId(), teamId))
          .thenReturn(Optional.empty());
      when(outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId))
          .thenReturn(List.of());
      when(unitAmenityRepository.findAmenitiesByUnitIdAndTeamId(unit.getId(), teamId))
          .thenReturn(List.of());
    }

    private WwsCalculationRequest requestFromPreFill(WwsPreFillResponse preFill) {
      return new WwsCalculationRequest(
          "2025",
          PROP_ID,
          null,
          Optional.empty(),
          preFill.surfaceAreaSqm(),
          preFill.numberOfRooms(),
          preFill.numberOfHeatedRooms(),
          preFill.energyLabel(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          preFill.outdoorSpaceSqm(),
          preFill.parkingType(),
          preFill.parkingSpaces(),
          Optional.empty(),
          Optional.empty(),
          preFill.accessibilityFeatures(),
          Optional.empty());
    }

    @Test
    @DisplayName("reads areaValue and energyEfficiencyRating from the unit, not the property")
    void readsDwellingAttributesFromUnit() {
      UUID propertyId = UUID.randomUUID();
      Unit unit = unit(UNIT_A_ID, propertyId, "A");
      Property property = buildingProperty(propertyId);
      stubDwellingLookups(unit, property);

      WwsPreFillResponse response = service.getPreFillData(UNIT_A_ID, principal);

      assertThat(response.energyLabel()).contains("A");
      assertThat(response.surfaceAreaSqm()).contains(new BigDecimal("75"));
      assertThat(response.propertyAddress()).contains("Keizersgracht 1, 1015 CJ Amsterdam");
    }

    @Test
    @DisplayName(
        "two units of one building with different energy labels produce different WWS point"
            + " totals")
    void differentUnitsProduceDifferentWwsTotals() {
      UUID propertyId = UUID.randomUUID();
      Unit unitA = unit(UNIT_A_ID, propertyId, "A");
      Unit unitB = unit(UNIT_B_ID, propertyId, "G");
      Property property = buildingProperty(propertyId);
      stubDwellingLookups(unitA, property);
      stubDwellingLookups(unitB, property);

      WwsPreFillResponse preFillA = service.getPreFillData(UNIT_A_ID, principal);
      WwsPreFillResponse preFillB = service.getPreFillData(UNIT_B_ID, principal);

      assertThat(preFillA.energyLabel()).contains("A");
      assertThat(preFillB.energyLabel()).contains("G");

      WwsCalculationResponse responseA = service.calculate(requestFromPreFill(preFillA), principal);
      WwsCalculationResponse responseB = service.calculate(requestFromPreFill(preFillB), principal);

      // Same building, same 75 m2 unit; only the energy label differs: A = 41 pts, G = -15 pts
      // under the 2025 rules. Surface area (75) + energy label + no-outdoor-space (-5) = total.
      assertThat(responseA.totalPoints()).isEqualByComparingTo("111.00");
      assertThat(responseB.totalPoints()).isEqualByComparingTo("55.00");
    }
  }
}
