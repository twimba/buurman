package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
}
