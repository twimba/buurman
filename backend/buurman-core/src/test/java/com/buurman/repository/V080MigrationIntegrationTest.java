package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.util.SidGenerator;

/**
 * V080 widens {@code uq_contracts_one_active_per_unit} (V072), which only ever covered {@code
 * status = 'ACTIVE'}, into {@code uq_contracts_one_in_force_per_unit}, covering {@code ACTIVE} and
 * {@code NOTICE_GIVEN} -- a NOTICE_GIVEN contract still occupies its unit. Its own pre-flight
 * {@code DO} block must abort the migration loudly on pre-existing data that would violate the
 * wider constraint, rather than failing obscurely (or silently succeeding) when the index itself is
 * created.
 */
@DisplayName("V080 widens the one-contract-per-unit index to cover NOTICE_GIVEN")
class V080MigrationIntegrationTest extends AbstractMigrationIntegrationTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

  @Test
  @DisplayName(
      "aborts rather than silently succeeding when a unit already carries both an ACTIVE and a"
          + " NOTICE_GIVEN contract")
  void abortsOnPreExistingActiveAndNoticeGivenOnTheSameUnit() {
    migrateTo("079");
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID unitId = seedUnit(propertyId, "1");
    seedContract(propertyId, unitId, "ACTIVE");
    seedContract(propertyId, unitId, "NOTICE_GIVEN");

    assertThatThrownBy(this::migrateToLatest)
        .hasMessageContaining("contracts")
        .hasMessageContaining("uq_contracts_one_in_force_per_unit");
  }

  @Test
  @DisplayName(
      "aborts rather than silently succeeding when a unit already carries two NOTICE_GIVEN"
          + " contracts")
  void abortsOnPreExistingDuplicateNoticeGivenContracts() {
    migrateTo("079");
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID unitId = seedUnit(propertyId, "1");
    seedContract(propertyId, unitId, "NOTICE_GIVEN");
    seedContract(propertyId, unitId, "NOTICE_GIVEN");

    assertThatThrownBy(this::migrateToLatest)
        .hasMessageContaining("contracts")
        .hasMessageContaining("uq_contracts_one_in_force_per_unit");
  }

  @Test
  @DisplayName(
      "replaces uq_contracts_one_active_per_unit with uq_contracts_one_in_force_per_unit, whose"
          + " predicate covers both ACTIVE and NOTICE_GIVEN")
  void replacesTheIndexWithAWiderPredicate() {
    migrateToLatest();

    assertThat(indexExists("contracts", "uq_contracts_one_active_per_unit")).isFalse();
    assertThat(indexExists("contracts", "uq_contracts_one_in_force_per_unit")).isTrue();
    assertThat(indexDefinition("contracts", "uq_contracts_one_in_force_per_unit"))
        .containsIgnoringCase("'ACTIVE'")
        .containsIgnoringCase("'NOTICE_GIVEN'")
        .containsIgnoringCase("deleted_at IS NULL");
  }

  @Test
  @DisplayName(
      "an ACTIVE and a NOTICE_GIVEN contract on the same unit collide at the DB level after the"
          + " migration, proving the backstop rather than just the index's existence")
  void rejectsActiveAndNoticeGivenTogetherAfterMigration() {
    migrateToLatest();
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID unitId = seedUnit(propertyId, "1");
    seedContract(propertyId, unitId, "ACTIVE");

    assertThatThrownBy(() -> seedContract(propertyId, unitId, "NOTICE_GIVEN"))
        .hasMessageContaining("uq_contracts_one_in_force_per_unit");
  }

  private void seedTeamAndUser() {
    dsl.insertInto(DSL.table("users"))
        .set(DSL.field("id", UUID.class), USER_ID)
        .set(DSL.field("identifier", String.class), "USR" + randomSuffix())
        .set(DSL.field("keycloak_id", String.class), "kc-" + USER_ID)
        .set(DSL.field("email", String.class), USER_ID.toString().substring(0, 8) + "@test.io")
        .set(DSL.field("first_name", String.class), "Test")
        .set(DSL.field("last_name", String.class), "User")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .onConflictDoNothing()
        .execute();
    dsl.insertInto(DSL.table("teams"))
        .set(DSL.field("id", UUID.class), TEAM_A_ID)
        .set(DSL.field("identifier", String.class), "TEA" + randomSuffix())
        .set(DSL.field("name", String.class), "Team A")
        .set(DSL.field("demo", Boolean.class), false)
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), USER_ID)
        .set(DSL.field("updated_by", UUID.class), USER_ID)
        .onConflictDoNothing()
        .execute();
  }

  private UUID seedProperty() {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("properties"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "PRO" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), TEAM_A_ID)
        .set(DSL.field("property_category", String.class), "RESIDENTIAL")
        .set(DSL.field("property_type", String.class), "APARTMENT")
        .set(DSL.field("street", String.class), "Main Street 1")
        .set(DSL.field("city", String.class), "Amsterdam")
        .set(DSL.field("postal_code", String.class), "1015 CJ")
        .set(DSL.field("country_code", String.class), "NL")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .execute();
    return id;
  }

  private UUID seedUnit(UUID propertyId, String unitNumber) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("units"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "UNT" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), TEAM_A_ID)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("unit_number", String.class), unitNumber)
        .set(DSL.field("unit_type", String.class), "APARTMENT")
        .set(DSL.field("status", String.class), "VACANT")
        .set(DSL.field("is_implicit", Boolean.class), false)
        .set(DSL.field("area_unit", String.class), "sqm")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .execute();
    return id;
  }

  private UUID seedContract(UUID propertyId, UUID unitId, String status) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("contracts"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newContractId().value())
        .set(DSL.field("team_id", UUID.class), TEAM_A_ID)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("unit_id", UUID.class), unitId)
        .set(DSL.field("contract_type", String.class), "FIXED_TERM")
        .set(DSL.field("start_date", LocalDate.class), LocalDate.of(2026, 1, 1))
        .set(DSL.field("rent_amount", Long.class), 100000L)
        .set(DSL.field("rent_amount_currency", String.class), "EUR")
        .set(DSL.field("payment_frequency", String.class), "MONTHLY")
        .set(DSL.field("status", String.class), status)
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), USER_ID)
        .set(DSL.field("updated_by", UUID.class), USER_ID)
        .execute();
    return id;
  }

  private boolean indexExists(String tableName, String indexName) {
    Integer count =
        dsl.selectCount()
            .from(DSL.table("pg_indexes"))
            .where(
                DSL.field("tablename", String.class)
                    .eq(tableName)
                    .and(DSL.field("indexname", String.class).eq(indexName)))
            .fetchOne(0, Integer.class);
    return count != null && count > 0;
  }

  private String indexDefinition(String tableName, String indexName) {
    return java.util.Objects.requireNonNull(
        dsl.select(DSL.field("indexdef", String.class))
            .from(DSL.table("pg_indexes"))
            .where(
                DSL.field("tablename", String.class)
                    .eq(tableName)
                    .and(DSL.field("indexname", String.class).eq(indexName)))
            .fetchOne(0, String.class),
        "expected index " + indexName + " on " + tableName + " to exist");
  }

  private static String randomSuffix() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase(Locale.ROOT);
  }
}
