package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitActiveTenancy;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.UpdateUnitRequest;
import com.buurman.dto.response.UnitGridRowResponse;
import com.buurman.dto.response.UnitResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.UnitMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitService")
class UnitServiceTest {

  @Mock private UnitRepository unitRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private UnitMapper unitMapper;

  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();

  @Nested
  @DisplayName("deleteUnit")
  class DeleteUnit {

    @Test
    @DisplayName("refuses to delete a property's last remaining unit")
    void refusesDeletingLastUnit() {
      Unit onlyUnit = unit("1", UnitStatus.VACANT);
      when(unitRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(onlyUnit);
      when(unitRepository.countActiveByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(1);

      assertThatThrownBy(() -> service().deleteUnit(unitIdentifier(), principal()))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("last unit");

      verify(unitRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    @DisplayName("deletes a unit when siblings remain")
    void deletesWhenSiblingsRemain() {
      Unit target = unit("2", UnitStatus.VACANT);
      when(unitRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(target);
      when(unitRepository.countActiveByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(4);
      when(contractRepository.countActiveByUnitId(target.getId(), TEAM_ID)).thenReturn(0);

      service().deleteUnit(unitIdentifier(), principal());

      verify(unitRepository).softDelete(target.getId(), TEAM_ID, USER_ID);
    }

    @Test
    @DisplayName("refuses to delete a unit that still has an active contract")
    void refusesDeletingUnitWithActiveContract() {
      Unit target = unit("2", UnitStatus.OCCUPIED);
      when(unitRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(target);
      when(unitRepository.countActiveByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(4);
      when(contractRepository.countActiveByUnitId(target.getId(), TEAM_ID)).thenReturn(1);

      assertThatThrownBy(() -> service().deleteUnit(unitIdentifier(), principal()))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("active contract");

      verify(unitRepository, never()).softDelete(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("createUnit")
  class CreateUnit {

    @Test
    @DisplayName("turns a duplicate unit number into a business-rule error, not a raw 500")
    void rejectsDuplicateUnitNumber() {
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitMapper.toEntity(any())).thenAnswer(inv -> unitFromCreateRequest(inv.getArgument(0)));
      when(unitRepository.save(any()))
          .thenThrow(new IntegrityConstraintViolationException("uq_units_property_number"));

      CreateUnitRequest request = createRequest("1");

      assertThatThrownBy(() -> service().createUnit(propertyIdentifier(), request, principal()))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("A unit numbered 1 already exists on this property.");
    }

    @Test
    @DisplayName(
        "promotes the implicit unit in place — same id and identifier — instead of copying it"
            + " into a new row")
    void promotesImplicitUnit() {
      Unit implicitUnit = unit("1", UnitStatus.VACANT);
      implicitUnit.setImplicit(true);
      UUID originalId = implicitUnit.getId();
      Sid originalIdentifier = implicitUnit.getIdentifier().orElseThrow();

      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(implicitUnit));
      when(unitMapper.toEntity(any())).thenAnswer(inv -> unitFromCreateRequest(inv.getArgument(0)));
      when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(unitMapper.toResponse(any(), any()))
          .thenAnswer(inv -> toResponseFixture(inv.getArgument(0), inv.getArgument(1)));

      CreateUnitRequest request = createRequest("2");

      UnitResponse response = service().createUnit(propertyIdentifier(), request, principal());

      // Two saves: the promoted implicit unit, then the new one.
      ArgumentCaptor<Unit> savedCaptor = ArgumentCaptor.forClass(Unit.class);
      verify(unitRepository, times(2)).save(savedCaptor.capture());
      Unit firstSaved = savedCaptor.getAllValues().get(0);

      // Promotion must flip the flag on the SAME row — an implementation that copies the implicit
      // unit into a new row (new id, identifier cleared) and saves that would also flip the flag
      // and issue two saves, passing a weaker check, while detaching every contract, payment,
      // photo and WWS calculation from that row's history.
      assertThat(firstSaved.getId()).isEqualTo(originalId);
      assertThat(firstSaved.getIdentifier()).contains(originalIdentifier);
      assertThat(firstSaved.isImplicit()).isFalse();
      assertThat(implicitUnit.isImplicit()).isFalse();
      assertThat(response.unitNumber()).isEqualTo("2");

      verify(unitRepository, never()).softDelete(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("updateUnit")
  class UpdateUnit {

    @Test
    @DisplayName("applies the update via UnitMapper and persists")
    void updatesUnit() {
      Unit existing = unit("1", UnitStatus.VACANT);
      when(unitRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(existing);
      when(unitRepository.save(existing)).thenReturn(existing);
      when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property());
      // Mimic what the real MapStruct-generated updateEntity would do, so the assertion below
      // proves the mapped value flows through to the response rather than just checking "not
      // null".
      doAnswer(
              inv -> {
                Unit target = inv.getArgument(0);
                UpdateUnitRequest req = inv.getArgument(1);
                target.setUnitNumber(req.unitNumber());
                return null;
              })
          .when(unitMapper)
          .updateEntity(any(), any());
      when(unitMapper.toResponse(any(), any()))
          .thenAnswer(inv -> toResponseFixture(inv.getArgument(0), inv.getArgument(1)));

      UpdateUnitRequest request = updateRequest("1B");

      UnitResponse response = service().updateUnit(unitIdentifier(), request, principal());

      verify(unitMapper).updateEntity(existing, request);
      verify(unitRepository).save(existing);
      assertThat(response.unitNumber()).isEqualTo("1B");
    }

    @Test
    @DisplayName("turns a duplicate unit number into a business-rule error on rename")
    void rejectsDuplicateUnitNumberOnRename() {
      Unit existing = unit("1", UnitStatus.VACANT);
      when(unitRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(existing);
      when(unitRepository.save(existing))
          .thenThrow(new IntegrityConstraintViolationException("uq_units_property_number"));

      UpdateUnitRequest request = updateRequest("3");

      assertThatThrownBy(() -> service().updateUnit(unitIdentifier(), request, principal()))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("A unit numbered 3 already exists on this property.");
    }
  }

  @Nested
  @DisplayName("getUnit")
  class GetUnit {

    @Test
    @DisplayName("returns the mapped unit scoped to the team")
    void returnsUnit() {
      Unit target = unit("1", UnitStatus.VACANT);
      when(unitRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(target);
      when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property());
      when(unitMapper.toResponse(any(), any()))
          .thenAnswer(inv -> toResponseFixture(inv.getArgument(0), inv.getArgument(1)));

      UnitResponse response = service().getUnit(unitIdentifier(), principal());

      assertThat(response.unitNumber()).isEqualTo("1");
    }
  }

  @Nested
  @DisplayName("listUnits")
  class ListUnits {

    @Test
    @DisplayName("fills tenant name and rent from the active contract, leaves vacancy days empty")
    void assemblesGridRows() {
      Unit occupied = unit("1", UnitStatus.OCCUPIED);
      Unit vacant = unit("2", UnitStatus.VACANT);
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(occupied, vacant));
      when(contractRepository.findActiveTenanciesByUnitIds(any(), eqTeam()))
          .thenReturn(
              List.of(
                  new UnitActiveTenancy(
                      occupied.getId(), new BigDecimal("1250.00"), "EUR", "Jane Tenant")));

      List<UnitGridRowResponse> rows = service().listUnits(propertyIdentifier(), principal());

      assertThat(rows).hasSize(2);

      UnitGridRowResponse occupiedRow =
          rows.stream().filter(r -> r.unitNumber().equals("1")).findFirst().orElseThrow();
      assertThat(occupiedRow.tenantName()).contains("Jane Tenant");
      assertThat(occupiedRow.monthlyRent()).contains(new BigDecimal("1250.00"));
      assertThat(occupiedRow.monthlyRentCurrency()).contains("EUR");
      assertThat(occupiedRow.vacancyDays()).isEmpty();

      UnitGridRowResponse vacantRow =
          rows.stream().filter(r -> r.unitNumber().equals("2")).findFirst().orElseThrow();
      assertThat(vacantRow.tenantName()).isEmpty();
      assertThat(vacantRow.monthlyRent()).isEmpty();
      assertThat(vacantRow.monthlyRentCurrency()).isEmpty();
      assertThat(vacantRow.vacancyDays()).isEmpty();
    }

    @Test
    @DisplayName(
        "keeps the most recently started tenancy when two active contracts exist for one unit,"
            + " instead of throwing on the duplicate key")
    void keepsMostRecentTenancyOnDuplicate() {
      Unit occupied = unit("1", UnitStatus.OCCUPIED);
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(occupied));
      // uq_contract_parties_contract_contact is not role-scoped and contracts.unit_id has only a
      // plain index, so the schema permits two ACTIVE contracts (or two PRIMARY_TENANT parties)
      // on one unit. findActiveTenanciesByUnitIds orders by start date descending, so the newer
      // tenancy is returned first — that's what the "keep first" merge below must pick.
      when(contractRepository.findActiveTenanciesByUnitIds(any(), eqTeam()))
          .thenReturn(
              List.of(
                  new UnitActiveTenancy(
                      occupied.getId(), new BigDecimal("1500.00"), "EUR", "New Tenant"),
                  new UnitActiveTenancy(
                      occupied.getId(), new BigDecimal("1200.00"), "EUR", "Old Tenant")));

      List<UnitGridRowResponse> rows = service().listUnits(propertyIdentifier(), principal());

      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).tenantName()).contains("New Tenant");
      assertThat(rows.get(0).monthlyRent()).contains(new BigDecimal("1500.00"));
    }
  }

  // --- fixtures ---

  private UnitService service() {
    return new UnitService(
        unitRepository, propertyRepository, contractRepository, unitMapper, clock);
  }

  private Unit unit(String number, UnitStatus status) {
    return Unit.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(SidGenerator.newUnitId()))
        .teamId(TEAM_ID)
        .propertyId(PROPERTY_ID)
        .unitNumber(number)
        .unitType(UnitType.APARTMENT)
        .status(status)
        .build();
  }

  private Unit unitFromCreateRequest(CreateUnitRequest request) {
    return Unit.builder().unitNumber(request.unitNumber()).unitType(request.unitType()).build();
  }

  private Property property() {
    return Property.builder()
        .id(PROPERTY_ID)
        .teamId(TEAM_ID)
        .identifier(Optional.of(SidGenerator.newPropertyId()))
        .build();
  }

  private UnitIdentifier unitIdentifier() {
    return UnitIdentifier.of(SidGenerator.newUnitId().value());
  }

  private PropertyIdentifier propertyIdentifier() {
    return PropertyIdentifier.of(SidGenerator.newPropertyId().value());
  }

  private UserPrincipal principal() {
    return new UserPrincipal(
        USER_ID,
        "usr_test",
        "kc-id",
        "test@example.com",
        "Test User",
        TEAM_ID,
        "team_test",
        TeamRole.TEAM_ADMIN,
        true);
  }

  private static UUID eqTeam() {
    return eq(TEAM_ID);
  }

  private CreateUnitRequest createRequest(String unitNumber) {
    return new CreateUnitRequest(
        unitNumber,
        Optional.empty(),
        Optional.empty(),
        UnitType.APARTMENT,
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
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private UpdateUnitRequest updateRequest(String unitNumber) {
    return new UpdateUnitRequest(
        unitNumber,
        Optional.empty(),
        Optional.empty(),
        UnitType.APARTMENT,
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
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private UnitResponse toResponseFixture(Unit unit, Sid propertyIdentifier) {
    return new UnitResponse(
        unit.getIdentifier().orElseThrow(),
        propertyIdentifier,
        unit.getUnitNumber(),
        unit.getName(),
        unit.getFloor(),
        unit.getUnitType(),
        unit.getStatus(),
        unit.isImplicit(),
        unit.getSortOrder(),
        unit.getAreaValue(),
        unit.getAreaUnit(),
        unit.getWozValue().map(MoneyAmount::value),
        unit.getWozValue().map(MoneyAmount::currency),
        unit.getWozSharePct(),
        unit.getAllocationShare(),
        unit.getEnergyEfficiencyRating(),
        unit.getEnergyCertificateExpiryDate(),
        unit.getHeatingType(),
        unit.getCoolingType(),
        unit.getHotWaterSystem(),
        unit.getInsulationNotes(),
        unit.getFlooringType(),
        unit.getWindowType(),
        unit.getHasSmokeDetectors(),
        unit.getHasCoDetectors(),
        unit.getHasFireExtinguisher(),
        unit.getHasAdaptedBathroom(),
        unit.getAccessibilityNotes(),
        unit.getCreatedAt().orElse(Instant.now(clock)),
        unit.getUpdatedAt());
  }
}
