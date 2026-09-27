package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.UnitRecordMapperImpl;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

@DisplayName("UnitRepository")
class UnitRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private UnitRepository repository;
  private UUID teamAPropertyId;
  private UUID teamBPropertyId;

  @BeforeEach
  void setUpRepository() {
    repository =
        new UnitRepository(dsl, TestDataHelper.wireMapper(new UnitRecordMapperImpl()), CLOCK);
    teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
  }

  @Test
  @DisplayName("round-trips a saved unit")
  void savesAndReadsBack() {
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("2")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .wozValue(Optional.of(MoneyAmount.of(new BigDecimal("425000.50"), "EUR")))
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());

    Unit found =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

    assertThat(found.getUnitNumber()).isEqualTo("2");
    assertThat(found.getUnitType()).isEqualTo(UnitType.APARTMENT);
    assertThat(found.getStatus()).isEqualTo(UnitStatus.VACANT);
    assertThat(found.getPropertyId()).isEqualTo(teamAPropertyId);
    MoneyAmount wozValue = found.getWozValue().orElseThrow();
    assertThat(wozValue.value()).isEqualByComparingTo(new BigDecimal("425000.50"));
    assertThat(wozValue.currency()).isEqualTo("EUR");
  }

  @Test
  @DisplayName("hides one team's units from another team's scoped lookup")
  void isolatesByTeam() {
    UUID teamBUnitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, teamBUnitId, teamBPropertyId, TEAM_B_ID, "1", "VACANT");
    String teamBIdentifier =
        Objects.requireNonNull(
            dsl.select(DSL.field("identifier", String.class))
                .from(DSL.table("units"))
                .where(DSL.field("id").eq(teamBUnitId))
                .fetchOne(0, String.class));

    assertThat(repository.findByIdentifierAndTeamId(Sid.of(teamBIdentifier), TEAM_A_ID)).isEmpty();
    assertThat(repository.findAllByPropertyIdAndTeamId(teamBPropertyId, TEAM_A_ID)).isEmpty();
    assertThat(repository.countActiveByTeamId(TEAM_A_ID)).isZero();
  }

  @Test
  @DisplayName("excludes units of a soft-deleted property from counts and listings")
  void excludesUnitsOfSoftDeletedProperty() {
    UUID unitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unitId, teamAPropertyId, TEAM_A_ID, "1", "VACANT");
    dsl.update(DSL.table("properties"))
        .set(DSL.field("deleted_at", LocalDateTime.class), LocalDateTime.now(CLOCK))
        .where(DSL.field("id").eq(teamAPropertyId))
        .execute();

    assertThat(repository.countActiveByTeamId(TEAM_A_ID)).isZero();
    assertThat(repository.findAllByPropertyIdAndTeamId(teamAPropertyId, TEAM_A_ID)).isEmpty();
    assertThat(repository.countActiveByPropertyIdAndTeamId(teamAPropertyId, TEAM_A_ID)).isZero();
  }

  @Test
  @DisplayName(
      "sums units across all of a team's properties for billing, excluding a soft-deleted "
          + "property's units (BUUR-106 Task 15)")
  void sumsUnitsAcrossPropertiesForBillingExcludingSoftDeletedProperty() {
    // Property 1 (active): 1 unit.
    TestDataHelper.insertUnit(dsl, UUID.randomUUID(), teamAPropertyId, TEAM_A_ID, "1", "VACANT");

    // Property 2 (active): 6 units.
    UUID secondPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    for (int i = 1; i <= 6; i++) {
      TestDataHelper.insertUnit(
          dsl, UUID.randomUUID(), secondPropertyId, TEAM_A_ID, String.valueOf(i), "VACANT");
    }

    // Property 3: soft-deleted, but still has units left behind (a prior migration deliberately
    // keeps implicit units around after a property is soft-deleted, so contracts retain a valid
    // foreign key). Those units must not inflate the billable count.
    UUID softDeletedPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    TestDataHelper.insertUnit(
        dsl, UUID.randomUUID(), softDeletedPropertyId, TEAM_A_ID, "1", "VACANT");
    TestDataHelper.insertUnit(
        dsl, UUID.randomUUID(), softDeletedPropertyId, TEAM_A_ID, "2", "VACANT");
    dsl.update(DSL.table("properties"))
        .set(DSL.field("deleted_at", LocalDateTime.class), LocalDateTime.now(CLOCK))
        .where(DSL.field("id").eq(softDeletedPropertyId))
        .execute();

    assertThat(repository.countActiveByTeamId(TEAM_A_ID)).isEqualTo(7);
  }

  @Test
  @DisplayName("excludes soft-deleted units from counts")
  void excludesSoftDeletedUnits() {
    UUID unitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unitId, teamAPropertyId, TEAM_A_ID, "1", "VACANT");

    repository.softDelete(unitId, TEAM_A_ID, USER_ID);

    assertThat(repository.countActiveByPropertyIdAndTeamId(teamAPropertyId, TEAM_A_ID)).isZero();
  }

  @Test
  @DisplayName("save() bumps version on every update, starting from 0 on insert (V072)")
  void saveIncrementsVersionOnEveryUpdate() {
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("2")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());
    assertThat(saved.getVersion()).isZero();

    saved.setStatus(UnitStatus.OCCUPIED);
    Unit afterFirstUpdate = repository.save(saved);
    assertThat(afterFirstUpdate.getVersion()).isEqualTo(1);

    afterFirstUpdate.setStatus(UnitStatus.VACANT);
    Unit afterSecondUpdate = repository.save(afterFirstUpdate);
    assertThat(afterSecondUpdate.getVersion()).isEqualTo(2);
  }

  @Test
  @DisplayName(
      "a stale-version save is rejected with a BusinessRuleException (409) instead of silently"
          + " overwriting a concurrent write -- the concurrent promote + edit race from BUUR-106"
          + " wave3c Critical 3")
  void rejectsAStaleVersionSave() {
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("2")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .implicit(true)
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());

    // Two independent in-memory copies of the same row, as two concurrent requests would each
    // hold after their own read.
    Unit t1Copy = repository.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
    Unit t2Copy = repository.getByIdAndTeamId(saved.getId(), TEAM_A_ID);

    // T1 promotes the unit (is_implicit -> false) and saves first.
    t1Copy.setImplicit(false);
    repository.save(t1Copy);

    // T2 still holds the pre-promotion snapshot (version 0, implicit still true) and tries to
    // save an unrelated edit. Before this fix, this UPDATE would blindly overwrite every column
    // -- including is_implicit -- resurrecting it back to true.
    t2Copy.setName(Optional.of("Back apartment"));
    assertThatThrownBy(() -> repository.save(t2Copy)).isInstanceOf(BusinessRuleException.class);

    Unit reloaded = repository.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
    assertThat(reloaded.isImplicit()).isFalse();
  }

  @Test
  @DisplayName("promotion (is_implicit -> false) still succeeds through the versioned save()")
  void promotionStillWorksThroughVersionedSave() {
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("1")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .implicit(true)
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());

    saved.setImplicit(false);
    Unit promoted = repository.save(saved);

    assertThat(promoted.isImplicit()).isFalse();
    assertThat(repository.getByIdAndTeamId(saved.getId(), TEAM_A_ID).isImplicit()).isFalse();
  }

  @Test
  @DisplayName(
      "save()'s UPDATE does not resurrect a soft-deleted unit (V072: DELETED_AT IS NULL guard)")
  void updateDoesNotResurrectASoftDeletedUnit() {
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("3")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());
    repository.softDelete(saved.getId(), TEAM_A_ID, USER_ID);

    saved.setStatus(UnitStatus.OCCUPIED);
    assertThatThrownBy(() -> repository.save(saved)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName(
      "softDeleteAllByPropertyIdAndTeamId soft-deletes every active unit of the property in one"
          + " statement, leaving another team's units untouched (BUUR-106 wave3c Critical 4)")
  void softDeleteAllByPropertyIdAndTeamIdSoftDeletesEveryUnitOfTheProperty() {
    UUID unit1Id = UUID.randomUUID();
    UUID unit2Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit1Id, teamAPropertyId, TEAM_A_ID, "1", "VACANT");
    TestDataHelper.insertUnit(dsl, unit2Id, teamAPropertyId, TEAM_A_ID, "2", "OCCUPIED");
    UUID otherTeamUnitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, otherTeamUnitId, teamBPropertyId, TEAM_B_ID, "1", "VACANT");

    int affected =
        repository.softDeleteAllByPropertyIdAndTeamId(teamAPropertyId, TEAM_A_ID, USER_ID);

    assertThat(affected).isEqualTo(2);
    assertThat(
            dsl.select(DSL.field("deleted_at", LocalDateTime.class))
                .from(DSL.table("units"))
                .where(DSL.field("id").eq(unit1Id))
                .fetchOne(0, LocalDateTime.class))
        .isNotNull();
    assertThat(
            dsl.select(DSL.field("deleted_at", LocalDateTime.class))
                .from(DSL.table("units"))
                .where(DSL.field("id").eq(unit2Id))
                .fetchOne(0, LocalDateTime.class))
        .isNotNull();
    assertThat(
            dsl.select(DSL.field("deleted_at", LocalDateTime.class))
                .from(DSL.table("units"))
                .where(DSL.field("id").eq(otherTeamUnitId))
                .fetchOne(0, LocalDateTime.class))
        .isNull();
  }

  @Test
  @DisplayName(
      "save()'s UPDATE branch round-trips every one of its 28 columns against a real PostgreSQL"
          + " row -- this UPDATE was previously executed zero times against a real database"
          + " (BUUR-106 follow-up register, section F, item 4)")
  void updateRoundTripsEveryColumn() {
    UUID otherPropertyInSameTeam = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);

    Unit inserted =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("1")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());

    // Mutate every settable column on the in-memory copy, including moving it to a different
    // property of the SAME team (property_id is one of the UPDATE's SET columns).
    inserted.setPropertyId(otherPropertyInSameTeam);
    inserted.setName(Optional.of("Garden flat"));
    inserted.setUnitNumber("1B");
    inserted.setFloor(Optional.of(2));
    inserted.setSortOrder(5);
    inserted.setUnitType(UnitType.COMMERCIAL);
    inserted.setStatus(UnitStatus.MAINTENANCE);
    inserted.setImplicit(true);
    inserted.setWozValue(Optional.of(MoneyAmount.of(new BigDecimal("512345.67"), "EUR")));
    inserted.setWozSharePct(Optional.of(new BigDecimal("42.5")));
    inserted.setAllocationShare(Optional.of(new BigDecimal("33.3")));
    inserted.setAreaValue(Optional.of(new BigDecimal("88.25")));
    inserted.setAreaUnit(Optional.of("sqft"));
    inserted.setEnergyEfficiencyRating(Optional.of("C"));
    inserted.setEnergyCertificateExpiryDate(Optional.of(java.time.LocalDate.of(2031, 6, 15)));
    inserted.setHeatingType(Optional.of("district"));
    inserted.setCoolingType(Optional.of("split-unit"));
    inserted.setHotWaterSystem(Optional.of("heat-pump"));
    inserted.setInsulationNotes(Optional.of("cavity wall"));
    inserted.setFlooringType(Optional.of("tile"));
    inserted.setWindowType(Optional.of("triple"));
    inserted.setHasSmokeDetectors(Optional.of(true));
    inserted.setHasCoDetectors(Optional.of(false));
    inserted.setHasFireExtinguisher(Optional.of(true));
    inserted.setHasAdaptedBathroom(Optional.of(false));
    inserted.setAccessibilityNotes(Optional.of("step-free entrance"));

    repository.save(inserted);

    // Re-fetch independently -- the save() return value is the same mutated in-memory instance,
    // so reading it back proves nothing about what actually landed in PostgreSQL.
    Unit reloaded = repository.getByIdAndTeamId(inserted.getId(), TEAM_A_ID);

    assertThat(reloaded.getPropertyId()).isEqualTo(otherPropertyInSameTeam);
    assertThat(reloaded.getName()).contains("Garden flat");
    assertThat(reloaded.getUnitNumber()).isEqualTo("1B");
    assertThat(reloaded.getFloor()).contains(2);
    assertThat(reloaded.getSortOrder()).isEqualTo(5);
    assertThat(reloaded.getUnitType()).isEqualTo(UnitType.COMMERCIAL);
    assertThat(reloaded.getStatus()).isEqualTo(UnitStatus.MAINTENANCE);
    assertThat(reloaded.isImplicit()).isTrue();
    assertThat(reloaded.getWozValue().orElseThrow().value())
        .isEqualByComparingTo(new BigDecimal("512345.67"));
    assertThat(reloaded.getWozValue().orElseThrow().currency()).isEqualTo("EUR");
    assertThat(reloaded.getWozSharePct().orElseThrow())
        .isEqualByComparingTo(new BigDecimal("42.5"));
    assertThat(reloaded.getAllocationShare().orElseThrow())
        .isEqualByComparingTo(new BigDecimal("33.3"));
    assertThat(reloaded.getAreaValue().orElseThrow()).isEqualByComparingTo(new BigDecimal("88.25"));
    assertThat(reloaded.getAreaUnit()).contains("sqft");
    assertThat(reloaded.getEnergyEfficiencyRating()).contains("C");
    assertThat(reloaded.getEnergyCertificateExpiryDate())
        .contains(java.time.LocalDate.of(2031, 6, 15));
    assertThat(reloaded.getHeatingType()).contains("district");
    assertThat(reloaded.getCoolingType()).contains("split-unit");
    assertThat(reloaded.getHotWaterSystem()).contains("heat-pump");
    assertThat(reloaded.getInsulationNotes()).contains("cavity wall");
    assertThat(reloaded.getFlooringType()).contains("tile");
    assertThat(reloaded.getWindowType()).contains("triple");
    assertThat(reloaded.getHasSmokeDetectors()).contains(true);
    assertThat(reloaded.getHasCoDetectors()).contains(false);
    assertThat(reloaded.getHasFireExtinguisher()).contains(true);
    assertThat(reloaded.getHasAdaptedBathroom()).contains(false);
    assertThat(reloaded.getAccessibilityNotes()).contains("step-free entrance");
    assertThat(reloaded.getVersion()).isEqualTo(1);
    assertThat(reloaded.getUpdatedBy()).contains(USER_ID);
  }

  @Test
  @DisplayName(
      "save() refuses to move a unit onto another team's property, on both INSERT and UPDATE"
          + " (BUUR-106 follow-up register, section F, cross-tenant writes)")
  void saveRejectsMovingAUnitToAnotherTeamsProperty() {
    // INSERT: a brand-new team-A unit whose propertyId points at team B's property.
    assertThatThrownBy(
            () ->
                repository.save(
                    Unit.builder()
                        .identifier(Optional.of(SidGenerator.newUnitId()))
                        .teamId(TEAM_A_ID)
                        .propertyId(teamBPropertyId)
                        .unitNumber("1")
                        .unitType(UnitType.APARTMENT)
                        .status(UnitStatus.VACANT)
                        .createdBy(Optional.of(USER_ID))
                        .updatedBy(Optional.of(USER_ID))
                        .build()))
        .isInstanceOf(BusinessRuleException.class);

    // UPDATE: an existing, correctly-scoped team-A unit whose propertyId is then mutated to
    // point at team B's property.
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("2")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .createdBy(Optional.of(USER_ID))
                .updatedBy(Optional.of(USER_ID))
                .build());

    saved.setPropertyId(teamBPropertyId);

    assertThatThrownBy(() -> repository.save(saved)).isInstanceOf(BusinessRuleException.class);

    // The unit was not moved: it is still attached to team A's property.
    Unit reloaded = repository.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
    assertThat(reloaded.getPropertyId()).isEqualTo(teamAPropertyId);
  }
}
