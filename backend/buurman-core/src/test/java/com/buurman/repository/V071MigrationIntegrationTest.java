package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.mapper.ContractRecordMapper;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * V071 adds composite FKs enforcing unit/property/team parent-consistency, plus a partial unique
 * index capping one ACTIVE contract per unit. Each is preceded by a pre-flight {@code DO} block
 * that must abort the migration loudly on pre-existing bad data, rather than failing obscurely (or
 * worse, silently succeeding) when the constraint itself is created.
 */
@DisplayName("V071 unit parent-consistency migration")
class V071MigrationIntegrationTest extends AbstractMigrationIntegrationTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

  @Test
  @DisplayName(
      "aborts rather than silently succeeding when a contract's unit belongs to a different"
          + " property")
  void abortsOnContractUnitPropertyMismatch() {
    migrateTo("069");
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID otherPropertyId = seedProperty();
    UUID unitId = seedUnit(propertyId, "1");
    seedContract(propertyId, unitId);
    // Corrupt it: point the contract at a unit of a *different* property, as V071's fk cannot yet
    // forbid on this schema version.
    dsl.update(DSL.table("contracts"))
        .set(DSL.field("property_id", UUID.class), otherPropertyId)
        .where(DSL.field("unit_id").eq(unitId))
        .execute();

    assertThatThrownBy(this::migrateToLatest)
        .hasMessageContaining("contracts")
        .hasMessageContaining("fk_contracts_unit_property");
  }

  @Test
  @DisplayName(
      "aborts rather than silently succeeding when a unit's team_id differs from its property's")
  void abortsOnUnitPropertyTeamMismatch() {
    migrateTo("069");
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID otherTeamId = UUID.randomUUID();
    TestDataHelper.insertTeam(dsl, otherTeamId, "Other Team", USER_ID);
    seedUnit(propertyId, "1");
    // Corrupt it: give the unit a team_id that does not match its own property's.
    dsl.update(DSL.table("units"))
        .set(DSL.field("team_id", UUID.class), otherTeamId)
        .where(DSL.field("property_id").eq(propertyId))
        .execute();

    assertThatThrownBy(this::migrateToLatest)
        .hasMessageContaining("units")
        .hasMessageContaining("fk_units_property_team");
  }

  @Test
  @DisplayName(
      "aborts rather than silently succeeding when a unit already carries two ACTIVE contracts")
  void abortsOnPreExistingDuplicateActiveContracts() {
    migrateTo("069");
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID unitId = seedUnit(propertyId, "1");
    seedContract(propertyId, unitId);
    seedContract(propertyId, unitId);

    assertThatThrownBy(this::migrateToLatest)
        .hasMessageContaining("contracts")
        .hasMessageContaining("uq_contracts_one_active_per_unit");
  }

  @Test
  @DisplayName(
      "findActiveByUnitId degrades to the oldest match instead of throwing on a legacy duplicate"
          + " that predates uq_contracts_one_active_per_unit")
  void findActiveByUnitIdDegradesGracefullyOnPreV071Duplicate() {
    migrateTo("069");
    seedTeamAndUser();
    UUID propertyId = seedProperty();
    UUID unitId = seedUnit(propertyId, "1");
    UUID olderContractId = seedContractAt(propertyId, unitId, NOW.minusDays(1));
    seedContractAt(propertyId, unitId, NOW);

    CountryMetadataSerializer countryMetadataSerializer =
        new CountryMetadataSerializer(new ObjectMapper());
    ContractRepository repository =
        new ContractRepository(
            dsl,
            new ContractRecordMapper(countryMetadataSerializer),
            countryMetadataSerializer,
            java.time.Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    assertThat(repository.findActiveByUnitId(unitId, TEAM_A_ID))
        .isPresent()
        .get()
        .extracting(Contract::getId)
        .isEqualTo(olderContractId);
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

  private UUID seedContract(UUID propertyId, UUID unitId) {
    return seedContractAt(propertyId, unitId, NOW);
  }

  private UUID seedContractAt(UUID propertyId, UUID unitId, LocalDateTime createdAt) {
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
        .set(DSL.field("status", String.class), "ACTIVE")
        .set(DSL.field("created_at", LocalDateTime.class), createdAt)
        .set(DSL.field("updated_at", LocalDateTime.class), createdAt)
        .set(DSL.field("created_by", UUID.class), USER_ID)
        .set(DSL.field("updated_by", UUID.class), USER_ID)
        .execute();
    return id;
  }

  @Test
  @DisplayName("index hygiene: drops the redundant units index, adds the hot-path composite index")
  void unitsIndexesAreCorrect() {
    migrateToLatest();

    assertThat(indexExists("units", "idx_units_property_id")).isFalse();
    assertThat(indexExists("units", "idx_units_team_property_status")).isTrue();
  }

  @Test
  @DisplayName("index hygiene: expense_allocations.team_id (ON DELETE CASCADE) is now indexed")
  void expenseAllocationsTeamIdIsIndexed() {
    migrateToLatest();

    assertThat(indexExists("expense_allocations", "idx_expense_allocations_team_id")).isTrue();
  }

  @Test
  @DisplayName(
      "index hygiene: photos/documents/expenses unit_id indexes are partial (unit_id IS NOT NULL)")
  void unitIdIndexesArePartial() {
    migrateToLatest();

    assertThat(indexDefinition("photos", "idx_photos_unit_id"))
        .containsIgnoringCase("unit_id IS NOT NULL");
    assertThat(indexDefinition("documents", "idx_documents_unit_id"))
        .containsIgnoringCase("unit_id IS NOT NULL");
    assertThat(indexDefinition("expenses", "idx_expenses_unit_id"))
        .containsIgnoringCase("unit_id IS NOT NULL");
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
    return Objects.requireNonNull(
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
    return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase();
  }
}
