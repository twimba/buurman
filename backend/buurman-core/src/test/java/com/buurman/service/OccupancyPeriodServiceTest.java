package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;
import com.buurman.domain.TeamRole;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.OccupancyPeriodIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.CreateOccupancyPeriodRequest;
import com.buurman.dto.request.EndOccupancyPeriodRequest;
import com.buurman.dto.response.OccupancyPeriodResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("OccupancyPeriodService")
class OccupancyPeriodServiceTest {

  @Mock private PropertyOccupancyPeriodRepository repository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PropertyAcquisitionRepository acquisitionRepository;
  @Mock private PropertyFinancingRepository financingRepository;

  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private OccupancyPeriodService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID UNIT_ID = UUID.randomUUID();
  private static final PropertyIdentifier PROPERTY_SID =
      PropertyIdentifier.of("prop_01JTEST000000000000000001");
  private static final UnitIdentifier UNIT_SID =
      UnitIdentifier.of("unit_01JTEST000000000000000001");

  private UserPrincipal principal;
  private Property property;
  private Unit unit;

  @BeforeEach
  void setUp() {
    service =
        new OccupancyPeriodService(
            repository,
            propertyRepository,
            unitRepository,
            contractRepository,
            contractExtensionRepository,
            acquisitionRepository,
            financingRepository,
            clock);

    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);

    property = new Property();
    property.setId(PROPERTY_ID);
    property.setIdentifier(Optional.of(PROPERTY_SID));

    unit =
        Unit.builder()
            .id(UNIT_ID)
            .identifier(Optional.of(UNIT_SID))
            .teamId(TEAM_ID)
            .propertyId(PROPERTY_ID)
            .unitNumber("1")
            .unitType(UnitType.APARTMENT)
            .status(UnitStatus.VACANT)
            .build();
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("creates period when no overlaps — captures saved entity")
    void createsWhenNoOverlaps() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(unit));
      when(repository.findOverlapping(
              eq(UNIT_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());
      when(contractRepository.findByPropertyId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(contractExtensionRepository.findByContractIdsAndTeamId(List.of(), TEAM_ID))
          .thenReturn(List.of());

      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 4, 1),
              OccupancyType.PERSONAL,
              Optional.of(LocalDate.of(2026, 6, 30)),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              null);

      OccupancyPeriodResponse response = service.create(PROPERTY_SID, request, principal);

      assertThat(response).isNotNull();
      assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
      assertThat(response.unitIdentifier()).isEqualTo(UNIT_SID);

      ArgumentCaptor<PropertyOccupancyPeriod> captor =
          ArgumentCaptor.forClass(PropertyOccupancyPeriod.class);
      verify(repository).save(captor.capture());
      PropertyOccupancyPeriod saved = captor.getValue();
      assertThat(saved.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(saved.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(saved.getUnitId()).isEqualTo(UNIT_ID);
      assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
      assertThat(saved.getEndDate()).isPresent().contains(LocalDate.of(2026, 6, 30));
      assertThat(saved.getType()).isEqualTo(OccupancyType.PERSONAL);
      assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
      assertThat(saved.getUpdatedBy()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("future start date does not change property status")
    void futureStartDateDoesNotChangePropertyStatus() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(unit));
      when(repository.findOverlapping(
              eq(UNIT_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());
      when(contractRepository.findByPropertyId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(contractExtensionRepository.findByContractIdsAndTeamId(List.of(), TEAM_ID))
          .thenReturn(List.of());

      // Start date is in the future (clock is 2026-03-01)
      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 5, 1),
              OccupancyType.PERSONAL,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              null);

      service.create(PROPERTY_SID, request, principal);

      verify(propertyRepository, never()).save(any(Property.class));
    }

    @Test
    @DisplayName("rejects two self-occupancy periods overlapping on the same unit")
    void rejectsOverlappingOnSameUnit() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(unit));

      PropertyOccupancyPeriod existing = new PropertyOccupancyPeriod();
      existing.setId(UUID.randomUUID());
      existing.setUnitId(UNIT_ID);
      when(repository.findOverlapping(
              eq(UNIT_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of(existing));

      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 4, 1),
              OccupancyType.PERSONAL,
              Optional.of(LocalDate.of(2026, 6, 30)),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              null);

      assertThatThrownBy(() -> service.create(PROPERTY_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage("Cannot create self-occupancy period: overlaps with an existing period");
    }

    @Test
    @DisplayName(
        "allows two overlapping self-occupancy periods on different units of the same property")
    void allowsOverlappingOnDifferentUnits() {
      UUID unitBId = UUID.randomUUID();
      UnitIdentifier unitBSid = UnitIdentifier.of("unit_01JTEST000000000000000002");
      Unit unitB =
          Unit.builder()
              .id(unitBId)
              .identifier(Optional.of(unitBSid))
              .teamId(TEAM_ID)
              .propertyId(PROPERTY_ID)
              .unitNumber("2")
              .unitType(UnitType.APARTMENT)
              .status(UnitStatus.VACANT)
              .build();

      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(unitRepository.getByIdentifierAndTeamId(UNIT_SID, TEAM_ID)).thenReturn(unit);
      when(unitRepository.getByIdentifierAndTeamId(unitBSid, TEAM_ID)).thenReturn(unitB);
      when(contractRepository.findByPropertyId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(contractExtensionRepository.findByContractIdsAndTeamId(List.of(), TEAM_ID))
          .thenReturn(List.of());

      // What the OLD, property-scoped implementation would have queried: an existing period
      // already recorded for this property. If the service regressed to keying findOverlapping()
      // by property_id instead of unit_id, both creates below would see this non-empty result and
      // throw — exactly the bug the unit_id re-scope (V068) fixes. Stubbed leniently: the whole
      // point of this test is that the (correct) implementation never calls this overload.
      PropertyOccupancyPeriod existingOnProperty = new PropertyOccupancyPeriod();
      existingOnProperty.setId(UUID.randomUUID());
      org.mockito.Mockito.lenient()
          .when(
              repository.findOverlapping(
                  eq(PROPERTY_ID),
                  eq(TEAM_ID),
                  any(LocalDate.class),
                  any(LocalDate.class),
                  isNull()))
          .thenReturn(List.of(existingOnProperty));
      // Unit-scoped queries: neither unit has an overlapping period of its own.
      when(repository.findOverlapping(
              eq(UNIT_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());
      when(repository.findOverlapping(
              eq(unitBId), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());

      CreateOccupancyPeriodRequest requestA =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 4, 1),
              OccupancyType.PERSONAL,
              Optional.of(LocalDate.of(2026, 6, 30)),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              UNIT_SID.value());
      CreateOccupancyPeriodRequest requestB =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 4, 15),
              OccupancyType.PERSONAL,
              Optional.of(LocalDate.of(2026, 7, 15)),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              unitBSid.value());

      OccupancyPeriodResponse responseA = service.create(PROPERTY_SID, requestA, principal);
      OccupancyPeriodResponse responseB = service.create(PROPERTY_SID, requestB, principal);

      assertThat(responseA.unitIdentifier()).isEqualTo(UNIT_SID);
      assertThat(responseB.unitIdentifier()).isEqualTo(unitBSid);
    }

    @Test
    @DisplayName(
        "does not persist property when period starts today or earlier"
            + " (status moved to units, BUUR-106 Task 12)")
    void setsPropertyStatusWhenStartsToday() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(unit));
      when(repository.findOverlapping(
              eq(UNIT_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());
      when(contractRepository.findByPropertyId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(contractExtensionRepository.findByContractIdsAndTeamId(List.of(), TEAM_ID))
          .thenReturn(List.of());

      // Start date is today (2026-03-01, matching our fixed clock)
      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 3, 1),
              OccupancyType.PERSONAL,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              null);

      service.create(PROPERTY_SID, request, principal);

      verify(propertyRepository, never()).save(any(Property.class));
    }
  }

  @Nested
  @DisplayName("end")
  class End {

    private static final OccupancyPeriodIdentifier PERIOD_SID =
        OccupancyPeriodIdentifier.of("occ_01JTEST000000000000000001");

    private PropertyOccupancyPeriod buildActivePeriod() {
      PropertyOccupancyPeriod period = new PropertyOccupancyPeriod();
      period.setId(UUID.randomUUID());
      period.setIdentifier(Optional.of(PERIOD_SID));
      period.setTeamId(TEAM_ID);
      period.setPropertyId(PROPERTY_ID);
      period.setUnitId(UNIT_ID);
      period.setStartDate(LocalDate.of(2026, 1, 1));
      period.setEndDate(Optional.empty());
      period.setType(OccupancyType.PERSONAL);
      period.setCreatedAt(Instant.now(clock));
      period.setUpdatedAt(Instant.now(clock));
      period.setCreatedBy(USER_ID);
      period.setUpdatedBy(USER_ID);
      return period;
    }

    @Test
    @DisplayName(
        "ends period and does not persist property (status moved to units, BUUR-106 Task 12)")
    void endsActivePeriod() {
      PropertyOccupancyPeriod period = buildActivePeriod();

      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(repository.getByIdentifierAndTeamId(PERIOD_SID, TEAM_ID)).thenReturn(period);
      when(unitRepository.getByIdAndTeamId(UNIT_ID, TEAM_ID)).thenReturn(unit);

      EndOccupancyPeriodRequest request =
          new EndOccupancyPeriodRequest(
              LocalDate.of(2026, 3, 1), Optional.empty(), Optional.empty());

      OccupancyPeriodResponse response = service.end(PROPERTY_SID, PERIOD_SID, request, principal);

      assertThat(response).isNotNull();
      assertThat(response.endDate()).isPresent().contains(LocalDate.of(2026, 3, 1));
      assertThat(response.unitIdentifier()).isEqualTo(UNIT_SID);
      verify(propertyRepository, never()).save(any(Property.class));
      verify(repository).save(period);
    }

    @Test
    @DisplayName("throws BusinessRuleException when end date is before start date")
    void throwsWhenEndDateBeforeStartDate() {
      PropertyOccupancyPeriod period = buildActivePeriod();
      period.setStartDate(LocalDate.of(2026, 2, 1));

      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(repository.getByIdentifierAndTeamId(PERIOD_SID, TEAM_ID)).thenReturn(period);

      EndOccupancyPeriodRequest request =
          new EndOccupancyPeriodRequest(
              LocalDate.of(2026, 1, 15), Optional.empty(), Optional.empty());

      assertThatThrownBy(() -> service.end(PROPERTY_SID, PERIOD_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("End date cannot be before start date");
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    private static final OccupancyPeriodIdentifier PERIOD_SID =
        OccupancyPeriodIdentifier.of("occ_01JTEST000000000000000002");

    @Test
    @DisplayName(
        "does not persist property when active period deleted (status moved to units,"
            + " BUUR-106 Task 12)")
    void revertsPropertyStatusToVacantWhenActivePeriodDeleted() {
      PropertyOccupancyPeriod period = new PropertyOccupancyPeriod();
      period.setId(UUID.randomUUID());
      period.setIdentifier(Optional.of(PERIOD_SID));
      period.setTeamId(TEAM_ID);
      period.setPropertyId(PROPERTY_ID);
      // Active: started in the past, no end date
      period.setStartDate(LocalDate.of(2026, 1, 1));
      period.setEndDate(Optional.empty());
      period.setType(OccupancyType.PERSONAL);

      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(repository.getByIdentifierAndTeamId(PERIOD_SID, TEAM_ID)).thenReturn(period);

      service.delete(PROPERTY_SID, PERIOD_SID, principal);

      verify(repository).softDeleteByIdAndTeamId(period.getId(), TEAM_ID);
      verify(propertyRepository, never()).save(any(Property.class));
    }
  }
}
