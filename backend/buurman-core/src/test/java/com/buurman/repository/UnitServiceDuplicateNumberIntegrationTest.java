package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Property;
import com.buurman.domain.TeamRole;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.UnitMapperImpl;
import com.buurman.mapper.UnitRecordMapperImpl;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.UnitService;
import com.buurman.util.FeatureFlags;

/**
 * {@code UnitService#saveOrTranslateDuplicate} catches ANY {@code
 * IntegrityConstraintViolationException} from {@code UnitRepository#save} and rewrites it as "A
 * unit numbered X already exists on this property" -- but {@code units} has a second CHECK
 * constraint ({@code chk_units_allocation_share}, 0-100) that jOOQ reports through the exact same
 * exception type. Against a mocked repository this distinction is invisible (BUUR-106 follow-up
 * register, section F, item 9); this test runs both cases against a real PostgreSQL database so the
 * two constraint violations are the genuine ones the database raises, not stand-ins.
 */
@DisplayName("UnitService.createUnit — duplicate unit number vs. unrelated CHECK violations")
class UnitServiceDuplicateNumberIntegrationTest extends AbstractRepositoryIntegrationTest {

  private UnitService unitService;
  private PropertyIdentifier propertyIdentifier;

  @BeforeEach
  void setUp() {
    UnitRepository unitRepository =
        new UnitRepository(dsl, TestDataHelper.wireMapper(new UnitRecordMapperImpl()), CLOCK);
    PropertyRepository propertyRepository =
        new PropertyRepository(
            dsl,
            TestDataHelper.wireMapper(new com.buurman.mapper.PropertyRecordMapperImpl()),
            CLOCK);

    FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    lenient()
        .when(featureFlagService.isEnabled(eq(FeatureFlags.MULTI_UNIT), any(UUID.class)))
        .thenReturn(true);

    unitService =
        new UnitService(
            unitRepository,
            propertyRepository,
            mock(com.buurman.repository.ContractRepository.class),
            mock(PropertyOccupancyPeriodRepository.class),
            mock(WwsCalculationRepository.class),
            mock(ExpenseAllocationRepository.class),
            TestDataHelper.wireMapper(new UnitMapperImpl()),
            CLOCK,
            featureFlagService);

    UUID propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    Property property = propertyRepository.getByIdAndTeamId(propertyId, TEAM_A_ID);
    propertyIdentifier = PropertyIdentifier.of(property.getIdentifier().orElseThrow().value());

    // A first unit numbered "1" so the second createUnit call below collides with it.
    unitService.createUnit(
        propertyIdentifier, createUnitRequest("1", Optional.empty()), principal());
  }

  @Test
  @DisplayName(
      "a real duplicate (property_id, unit_number) is translated to the friendly"
          + " already-exists message")
  void realDuplicateUnitNumberIsTranslated() {
    assertThatThrownBy(
            () ->
                unitService.createUnit(
                    propertyIdentifier, createUnitRequest("1", Optional.empty()), principal()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A unit numbered 1 already exists on this property.");
  }

  @Test
  @DisplayName(
      "an out-of-range allocationShare (150) violates chk_units_allocation_share and must NOT be"
          + " reported as a duplicate unit number")
  void outOfRangeAllocationShareIsNotReportedAsDuplicate() {
    assertThatThrownBy(
            () ->
                unitService.createUnit(
                    propertyIdentifier,
                    createUnitRequest("2", Optional.of(new BigDecimal("150"))),
                    principal()))
        .isInstanceOf(RuntimeException.class)
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .as("must not misreport an allocation-share CHECK violation as a duplicate")
                    .doesNotContain("already exists on this property"));
  }

  private CreateUnitRequest createUnitRequest(
      String unitNumber, Optional<BigDecimal> allocationShare) {
    return new CreateUnitRequest(
        unitNumber, // unitNumber
        Optional.empty(), // name
        Optional.empty(), // floor
        UnitType.APARTMENT, // unitType
        Optional.empty(), // status
        Optional.empty(), // areaValue
        Optional.empty(), // areaUnit
        Optional.empty(), // wozValue
        Optional.empty(), // wozValueCurrency
        allocationShare, // allocationShare
        Optional.empty(), // wozSharePct
        Optional.empty(), // energyEfficiencyRating
        Optional.empty(), // energyCertificateExpiryDate
        Optional.empty(), // heatingType
        Optional.empty(), // coolingType
        Optional.empty(), // hotWaterSystem
        Optional.empty(), // insulationNotes
        Optional.empty(), // flooringType
        Optional.empty(), // windowType
        Optional.empty(), // hasSmokeDetectors
        Optional.empty(), // hasCoDetectors
        Optional.empty(), // hasFireExtinguisher
        Optional.empty(), // hasAdaptedBathroom
        Optional.empty()); // accessibilityNotes
  }

  private UserPrincipal principal() {
    return new UserPrincipal(
        USER_ID,
        "usr_test",
        "kc-123",
        "test@example.com",
        "Test User",
        TEAM_A_ID,
        "team_test",
        TeamRole.TEAM_ADMIN);
  }
}
