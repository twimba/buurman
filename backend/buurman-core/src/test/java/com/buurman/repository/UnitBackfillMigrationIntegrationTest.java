package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V068 units backfill")
class UnitBackfillMigrationIntegrationTest extends AbstractMigrationIntegrationTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

  @Test
  @DisplayName("gives every property exactly one implicit unit carrying its dwelling data")
  void backfillsImplicitUnitPerProperty() {
    migrateTo("067");
    UUID propertyId =
        seedProperty(TEAM_A_ID, "Keizersgracht 12", "OCCUPIED", new BigDecimal("85.50"), "B");

    migrateToLatest();

    var units =
        dsl.select(
                DSL.field("identifier", String.class),
                DSL.field("is_implicit", Boolean.class),
                DSL.field("unit_number", String.class),
                DSL.field("status", String.class),
                DSL.field("area_value", BigDecimal.class),
                DSL.field("energy_efficiency_rating", String.class),
                DSL.field("unit_type", String.class),
                DSL.field("allocation_share", BigDecimal.class))
            .from(DSL.table("units"))
            .where(DSL.field("property_id").eq(propertyId))
            .fetch();

    assertThat(units).hasSize(1);
    assertThat(units.get(0).value1()).startsWith("UNT").hasSize(29);
    assertThat(units.get(0).value2()).isTrue();
    assertThat(units.get(0).value3()).isEqualTo("1");
    assertThat(units.get(0).value4()).isEqualTo("OCCUPIED");
    assertThat(units.get(0).value5()).isEqualByComparingTo("85.50");
    assertThat(units.get(0).value6()).isEqualTo("B");
    assertThat(units.get(0).value7()).isEqualTo("APARTMENT");
    assertThat(units.get(0).value8()).isEqualByComparingTo("100");
  }

  @Test
  @DisplayName("points every contract at a unit of its own property")
  void backfillsContractUnitId() {
    migrateTo("067");
    UUID propertyId =
        seedProperty(TEAM_A_ID, "Prinsengracht 4", "OCCUPIED", new BigDecimal("70"), "C");
    UUID contractId = seedContract(TEAM_A_ID, propertyId);

    migrateToLatest();

    UUID contractUnitId =
        dsl.select(DSL.field("unit_id", UUID.class))
            .from(DSL.table("contracts"))
            .where(DSL.field("id").eq(contractId))
            .fetchOne(DSL.field("unit_id", UUID.class));

    assertThat(contractUnitId).isNotNull();

    UUID unitPropertyId =
        dsl.select(DSL.field("property_id", UUID.class))
            .from(DSL.table("units"))
            .where(DSL.field("id").eq(contractUnitId))
            .fetchOne(DSL.field("property_id", UUID.class));

    assertThat(unitPropertyId).isEqualTo(propertyId);
  }

  @Test
  @DisplayName("backfills soft-deleted properties too, so no contract loses its FK target")
  void backfillsSoftDeletedProperties() {
    migrateTo("067");
    UUID propertyId = seedProperty(TEAM_A_ID, "Gone Street 1", "VACANT", new BigDecimal("40"), "G");
    dsl.update(DSL.table("properties"))
        .set(DSL.field("deleted_at", LocalDateTime.class), NOW)
        .where(DSL.field("id").eq(propertyId))
        .execute();

    migrateToLatest();

    Integer unitCount =
        dsl.selectCount()
            .from(DSL.table("units"))
            .where(DSL.field("property_id").eq(propertyId))
            .fetchOne(0, Integer.class);

    assertThat(unitCount).isEqualTo(1);
  }

  @Test
  @DisplayName("keeps one team's backfilled units invisible to another team's scoped query")
  void backfilledUnitsAreTeamScoped() {
    migrateTo("067");
    seedProperty(TEAM_A_ID, "A Street 1", "VACANT", new BigDecimal("50"), "A");
    seedProperty(TEAM_B_ID, "B Street 2", "VACANT", new BigDecimal("60"), "B");

    migrateToLatest();

    Integer teamAUnits =
        dsl.selectCount()
            .from(DSL.table("units"))
            .where(DSL.field("team_id").eq(TEAM_A_ID))
            .fetchOne(0, Integer.class);

    assertThat(teamAUnits).isEqualTo(1);
  }

  // --- seeding helpers, written against the V067 schema ---

  private UUID seedProperty(
      UUID teamId, String street, String status, BigDecimal area, String energyRating) {
    seedTeamAndUser(teamId);
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("properties"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "PRO" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_category", String.class), "RESIDENTIAL")
        .set(DSL.field("property_type", String.class), "APARTMENT")
        .set(DSL.field("status", String.class), status)
        .set(DSL.field("street", String.class), street)
        .set(DSL.field("city", String.class), "Amsterdam")
        .set(DSL.field("postal_code", String.class), "1015 CJ")
        .set(DSL.field("country_code", String.class), "NL")
        .set(DSL.field("area_value", BigDecimal.class), area)
        .set(DSL.field("area_unit", String.class), "sqm")
        .set(DSL.field("energy_efficiency_rating", String.class), energyRating)
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .execute();
    return id;
  }

  private UUID seedContract(UUID teamId, UUID propertyId) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("contracts"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "CON" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("contract_type", String.class), "FIXED_TERM")
        .set(DSL.field("start_date", LocalDate.class), LocalDate.of(2026, 1, 1))
        .set(DSL.field("rent_amount", Long.class), 150000L)
        .set(DSL.field("rent_amount_currency", String.class), "EUR")
        .set(DSL.field("payment_frequency", String.class), "MONTHLY")
        .set(DSL.field("status", String.class), "ACTIVE")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), USER_ID)
        .set(DSL.field("updated_by", UUID.class), USER_ID)
        .execute();
    return id;
  }

  private void seedTeamAndUser(UUID teamId) {
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
        .set(DSL.field("id", UUID.class), teamId)
        .set(DSL.field("identifier", String.class), "TEA" + randomSuffix())
        .set(DSL.field("name", String.class), "Team " + teamId.toString().substring(0, 4))
        .set(DSL.field("demo", Boolean.class), false)
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), USER_ID)
        .set(DSL.field("updated_by", UUID.class), USER_ID)
        .onConflictDoNothing()
        .execute();
  }

  private static String randomSuffix() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase();
  }
}
