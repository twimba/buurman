package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V070 units backfill")
class UnitBackfillMigrationIntegrationTest extends AbstractMigrationIntegrationTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

  /**
   * A second actor, so {@code created_by} / {@code updated_by} differ between the two properties of
   * the copy tests. With a single user id, a backfill that bound {@code created_by} where {@code
   * updated_by} belonged (or dropped one entirely and let the column default) would still pass.
   */
  private static final UUID OTHER_USER_ID = UUID.randomUUID();

  /**
   * Every column V070's {@code INSERT INTO units ... SELECT ... FROM properties} copies off the
   * property, plus the two it derives ({@code unit_type} from {@code property_category}, {@code
   * area_unit} via {@code coalesce}). Two instances with pairwise-different values are seeded so a
   * transposed pair of columns cannot pass.
   */
  private record PropertyDwelling(
      String propertyCategory,
      String propertyType,
      String status,
      BigDecimal areaValue,
      String areaUnit,
      String energyEfficiencyRating,
      LocalDate energyCertificateExpiryDate,
      String heatingType,
      String coolingType,
      String hotWaterSystem,
      String insulationNotes,
      String flooringType,
      String windowType,
      boolean hasSmokeDetectors,
      boolean hasCoDetectors,
      boolean hasFireExtinguisher,
      boolean hasAdaptedBathroom,
      String accessibilityNotes,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      UUID createdBy,
      UUID updatedBy,
      String expectedUnitType) {}

  private static final PropertyDwelling DWELLING_ONE =
      new PropertyDwelling(
          "RESIDENTIAL",
          "APARTMENT",
          "OCCUPIED",
          new BigDecimal("85.50"),
          "sqm",
          "B",
          LocalDate.of(2031, 4, 5),
          "DISTRICT_HEATING",
          "CENTRAL_AC",
          "BOILER",
          "Roof insulated in 2019",
          "HARDWOOD",
          "TRIPLE_GLAZING",
          true,
          true,
          false,
          true,
          "Step-free entrance from the street",
          LocalDateTime.of(2024, 1, 2, 3, 4),
          LocalDateTime.of(2025, 5, 6, 7, 8),
          USER_ID,
          OTHER_USER_ID,
          "APARTMENT");

  private static final PropertyDwelling DWELLING_TWO =
      new PropertyDwelling(
          "COMMERCIAL",
          "OFFICE",
          "UNDER_RENOVATION",
          new BigDecimal("42.25"),
          "sqft",
          "F",
          LocalDate.of(2027, 11, 30),
          "HEAT_PUMP",
          "NONE",
          "ELECTRIC_BOILER",
          "Single-skin walls, uninsulated",
          "CONCRETE",
          "SINGLE_GLAZING",
          false,
          false,
          true,
          false,
          "Third floor, no lift",
          LocalDateTime.of(2022, 9, 10, 11, 12),
          LocalDateTime.of(2023, 12, 13, 14, 15),
          OTHER_USER_ID,
          USER_ID,
          "COMMERCIAL");

  /** Every column V070's {@code INSERT INTO unit_residential_details ... SELECT} copies. */
  private record ResidentialDetails(
      Integer bedrooms,
      Integer bathrooms,
      boolean furnished,
      String petPolicy,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      UUID createdBy,
      UUID updatedBy) {}

  private static final ResidentialDetails DETAILS_ONE =
      new ResidentialDetails(
          3,
          2,
          true,
          "ALLOWED",
          LocalDateTime.of(2021, 2, 3, 4, 5),
          LocalDateTime.of(2022, 3, 4, 5, 6),
          USER_ID,
          OTHER_USER_ID);

  private static final ResidentialDetails DETAILS_TWO =
      new ResidentialDetails(
          1,
          4,
          false,
          "CATS_ONLY",
          LocalDateTime.of(2019, 6, 7, 8, 9),
          LocalDateTime.of(2020, 7, 8, 9, 10),
          OTHER_USER_ID,
          USER_ID);

  /** Every column V070's {@code INSERT INTO unit_amenities ... SELECT} copies. */
  private record AmenityLink(
      String amenityName,
      String notes,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      UUID createdBy,
      UUID updatedBy,
      @Nullable LocalDateTime deletedAt) {}

  private static final AmenityLink AMENITY_ONE =
      new AmenityLink(
          "SWIMMING_POOL",
          "Shared, open May to September",
          LocalDateTime.of(2018, 1, 1, 1, 1),
          LocalDateTime.of(2019, 2, 2, 2, 2),
          USER_ID,
          OTHER_USER_ID,
          null);

  private static final AmenityLink AMENITY_TWO =
      new AmenityLink(
          "GYM",
          "Removed after the 2024 refit",
          LocalDateTime.of(2016, 3, 3, 3, 3),
          LocalDateTime.of(2017, 4, 4, 4, 4),
          OTHER_USER_ID,
          USER_ID,
          LocalDateTime.of(2024, 5, 5, 5, 5));

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
  @DisplayName(
      "copies every column of the units INSERT ... SELECT, on two properties holding different"
          + " values, so a transposed or dropped column cannot survive the DROP COLUMN that"
          + " follows it")
  void copiesEveryDwellingColumnOntoTheImplicitUnit() {
    migrateTo("067");
    seedOtherUser();
    UUID propertyOneId = seedDwellingProperty(TEAM_A_ID, "Copy Street 1", DWELLING_ONE);
    UUID propertyTwoId = seedDwellingProperty(TEAM_A_ID, "Copy Street 2", DWELLING_TWO);

    migrateToLatest();

    assertDwellingCopiedOnto(propertyOneId, TEAM_A_ID, DWELLING_ONE);
    assertDwellingCopiedOnto(propertyTwoId, TEAM_A_ID, DWELLING_TWO);
  }

  @Test
  @DisplayName(
      "copies every column of the unit_residential_details INSERT ... SELECT, on two properties"
          + " holding different values, before property_residential_details is dropped")
  void copiesEveryResidentialDetailColumnOntoTheImplicitUnit() {
    migrateTo("067");
    seedOtherUser();
    UUID propertyOneId = seedDwellingProperty(TEAM_A_ID, "Details Street 1", DWELLING_ONE);
    UUID propertyTwoId = seedDwellingProperty(TEAM_A_ID, "Details Street 2", DWELLING_TWO);
    seedResidentialDetails(TEAM_A_ID, propertyOneId, DETAILS_ONE);
    seedResidentialDetails(TEAM_A_ID, propertyTwoId, DETAILS_TWO);

    migrateToLatest();

    assertResidentialDetailsCopiedOnto(propertyOneId, TEAM_A_ID, DETAILS_ONE);
    assertResidentialDetailsCopiedOnto(propertyTwoId, TEAM_A_ID, DETAILS_TWO);
  }

  @Test
  @DisplayName(
      "copies every column of the unit_amenities INSERT ... SELECT, on two properties holding"
          + " different values, before property_amenities is dropped")
  void copiesEveryAmenityLinkColumnOntoTheImplicitUnit() {
    migrateTo("067");
    seedOtherUser();
    UUID propertyOneId = seedDwellingProperty(TEAM_A_ID, "Amenity Street 1", DWELLING_ONE);
    UUID propertyTwoId = seedDwellingProperty(TEAM_A_ID, "Amenity Street 2", DWELLING_TWO);
    seedAmenityLink(TEAM_A_ID, propertyOneId, AMENITY_ONE);
    seedAmenityLink(TEAM_A_ID, propertyTwoId, AMENITY_TWO);

    migrateToLatest();

    assertAmenityLinkCopiedOnto(propertyOneId, TEAM_A_ID, AMENITY_ONE);
    assertAmenityLinkCopiedOnto(propertyTwoId, TEAM_A_ID, AMENITY_TWO);
  }

  @Test
  @DisplayName("gives each backfilled unit its own property's dwelling data, not another's")
  void backfillsDistinctDwellingDataPerProperty() {
    migrateTo("067");
    UUID propertyOneId =
        seedProperty(TEAM_A_ID, "Herengracht 1", "OCCUPIED", new BigDecimal("85.50"), "B");
    UUID propertyTwoId =
        seedProperty(TEAM_A_ID, "Herengracht 2", "VACANT", new BigDecimal("42.00"), "F");

    migrateToLatest();

    assertUnitMatchesOwnProperty(propertyOneId, "OCCUPIED", new BigDecimal("85.50"), "B");
    assertUnitMatchesOwnProperty(propertyTwoId, "VACANT", new BigDecimal("42.00"), "F");
  }

  @Test
  @DisplayName(
      "points each occupancy period at a unit of its OWN property — two properties, two distinct"
          + " units, never one arbitrary building for both")
  void backfillsOccupancyPeriodUnitId() {
    migrateTo("067");
    UUID propertyOneId =
        seedProperty(TEAM_A_ID, "Bloemgracht 5", "SELF_OCCUPIED", new BigDecimal("60"), "D");
    UUID propertyTwoId =
        seedProperty(TEAM_A_ID, "Bloemgracht 7", "OCCUPIED", new BigDecimal("61"), "C");
    UUID occupancyOneId = seedOccupancyPeriod(TEAM_A_ID, propertyOneId);
    UUID occupancyTwoId = seedOccupancyPeriod(TEAM_A_ID, propertyTwoId);

    migrateToLatest();

    assertBackfilledUnitBelongsToOwnProperty(
        "property_occupancy_periods", occupancyOneId, propertyOneId, occupancyTwoId, propertyTwoId);
  }

  @Test
  @DisplayName(
      "points each WWS calculation at a unit of its OWN property — two properties, two distinct"
          + " units, never one arbitrary building for both")
  void backfillsWwsCalculationUnitId() {
    migrateTo("067");
    UUID propertyOneId =
        seedProperty(TEAM_A_ID, "Bloemgracht 6", "OCCUPIED", new BigDecimal("65"), "E");
    UUID propertyTwoId =
        seedProperty(TEAM_A_ID, "Bloemgracht 8", "VACANT", new BigDecimal("66"), "A");
    UUID wwsOneId = seedWwsCalculation(TEAM_A_ID, propertyOneId);
    UUID wwsTwoId = seedWwsCalculation(TEAM_A_ID, propertyTwoId);

    migrateToLatest();

    assertBackfilledUnitBelongsToOwnProperty(
        "wws_calculations", wwsOneId, propertyOneId, wwsTwoId, propertyTwoId);
  }

  @Test
  @DisplayName(
      "points each contract at a unit of its OWN property — two properties, two distinct units,"
          + " never one arbitrary building for both")
  void backfillsContractUnitId() {
    migrateTo("067");
    UUID propertyOneId =
        seedProperty(TEAM_A_ID, "Prinsengracht 4", "OCCUPIED", new BigDecimal("70"), "C");
    UUID propertyTwoId =
        seedProperty(TEAM_A_ID, "Prinsengracht 6", "OCCUPIED", new BigDecimal("71"), "B");
    UUID contractOneId = seedContract(TEAM_A_ID, propertyOneId);
    UUID contractTwoId = seedContract(TEAM_A_ID, propertyTwoId);

    migrateToLatest();

    assertBackfilledUnitBelongsToOwnProperty(
        "contracts", contractOneId, propertyOneId, contractTwoId, propertyTwoId);
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

    // Not merely non-null: the backfilled unit's deleted_at must equal the property's own, since
    // the copy is a straight column copy (see the migration's SELECT p.deleted_at FROM properties
    // p), not just "happens to be set".
    LocalDateTime unitDeletedAt =
        dsl.select(DSL.field("deleted_at", LocalDateTime.class))
            .from(DSL.table("units"))
            .where(DSL.field("property_id").eq(propertyId))
            .fetchOne(0, LocalDateTime.class);
    assertThat(unitDeletedAt).isEqualTo(NOW);
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

  /**
   * Asserts the per-property identity the old {@code isNotNull()} assertions missed: each row's
   * backfilled {@code unit_id} resolves to a unit of that row's <em>own</em> property, and the two
   * rows resolved to different units. A backfill whose {@code UPDATE ... FROM units} lost its
   * {@code u.property_id = <table>.property_id} predicate would attach both rows to one arbitrary
   * building; that breaks the second and third assertions here.
   */
  private void assertBackfilledUnitBelongsToOwnProperty(
      String table, UUID rowOneId, UUID propertyOneId, UUID rowTwoId, UUID propertyTwoId) {
    UUID unitOneId = backfilledUnitId(table, rowOneId);
    UUID unitTwoId = backfilledUnitId(table, rowTwoId);

    assertThat(unitOneId)
        .as("%s rows of two different properties must not share one unit", table)
        .isNotEqualTo(unitTwoId);
    assertThat(propertyIdOfUnit(unitOneId))
        .as("%s row 1's unit must belong to property 1", table)
        .isEqualTo(propertyOneId);
    assertThat(propertyIdOfUnit(unitTwoId))
        .as("%s row 2's unit must belong to property 2", table)
        .isEqualTo(propertyTwoId);
  }

  private UUID backfilledUnitId(String table, UUID rowId) {
    return Objects.requireNonNull(
        dsl.select(DSL.field("unit_id", UUID.class))
            .from(DSL.table(table))
            .where(DSL.field("id").eq(rowId))
            .fetchOne(0, UUID.class),
        table + " row " + rowId + " has no backfilled unit_id");
  }

  private UUID implicitUnitId(UUID propertyId) {
    return Objects.requireNonNull(
        dsl.select(DSL.field("id", UUID.class))
            .from(DSL.table("units"))
            .where(
                DSL.field("property_id")
                    .eq(propertyId)
                    .and(DSL.field("is_implicit", Boolean.class).isTrue()))
            .fetchOne(0, UUID.class),
        "expected exactly one implicit unit for property " + propertyId);
  }

  private UUID propertyIdOfUnit(UUID unitId) {
    return Objects.requireNonNull(
        dsl.select(DSL.field("property_id", UUID.class))
            .from(DSL.table("units"))
            .where(DSL.field("id").eq(unitId))
            .fetchOne(0, UUID.class),
        "no unit row for id " + unitId);
  }

  private void assertDwellingCopiedOnto(UUID propertyId, UUID teamId, PropertyDwelling expected) {
    Record unit =
        Objects.requireNonNull(
            dsl.select(DSL.asterisk())
                .from(DSL.table("units"))
                .where(DSL.field("property_id").eq(propertyId))
                .fetchOne(),
            "expected exactly one backfilled unit for property " + propertyId);

    assertThat(unit.get("team_id", UUID.class)).isEqualTo(teamId);
    assertThat(unit.get("property_id", UUID.class)).isEqualTo(propertyId);
    assertThat(unit.get("identifier", String.class)).startsWith("UNT").hasSize(29);
    assertThat(unit.get("unit_number", String.class)).isEqualTo("1");
    assertThat(unit.get("name", String.class)).isNull();
    assertThat(unit.get("floor", Integer.class)).isNull();
    assertThat(unit.get("sort_order", Integer.class)).isZero();
    assertThat(unit.get("is_implicit", Boolean.class)).isTrue();
    assertThat(unit.get("allocation_share", BigDecimal.class)).isEqualByComparingTo("100");
    assertThat(unit.get("woz_value", Long.class)).isNull();
    assertThat(unit.get("woz_value_currency", String.class)).isNull();
    assertThat(unit.get("woz_share_pct", BigDecimal.class)).isNull();

    assertThat(unit.get("unit_type", String.class)).isEqualTo(expected.expectedUnitType());
    assertThat(unit.get("status", String.class)).isEqualTo(expected.status());
    assertThat(unit.get("area_value", BigDecimal.class)).isEqualByComparingTo(expected.areaValue());
    assertThat(unit.get("area_unit", String.class)).isEqualTo(expected.areaUnit());
    assertThat(unit.get("energy_efficiency_rating", String.class))
        .isEqualTo(expected.energyEfficiencyRating());
    assertThat(unit.get("energy_certificate_expiry_date", LocalDate.class))
        .isEqualTo(expected.energyCertificateExpiryDate());
    assertThat(unit.get("heating_type", String.class)).isEqualTo(expected.heatingType());
    assertThat(unit.get("cooling_type", String.class)).isEqualTo(expected.coolingType());
    assertThat(unit.get("hot_water_system", String.class)).isEqualTo(expected.hotWaterSystem());
    assertThat(unit.get("insulation_notes", String.class)).isEqualTo(expected.insulationNotes());
    assertThat(unit.get("flooring_type", String.class)).isEqualTo(expected.flooringType());
    assertThat(unit.get("window_type", String.class)).isEqualTo(expected.windowType());
    assertThat(unit.get("has_smoke_detectors", Boolean.class))
        .isEqualTo(expected.hasSmokeDetectors());
    assertThat(unit.get("has_co_detectors", Boolean.class)).isEqualTo(expected.hasCoDetectors());
    assertThat(unit.get("has_fire_extinguisher", Boolean.class))
        .isEqualTo(expected.hasFireExtinguisher());
    assertThat(unit.get("has_adapted_bathroom", Boolean.class))
        .isEqualTo(expected.hasAdaptedBathroom());
    assertThat(unit.get("accessibility_notes", String.class))
        .isEqualTo(expected.accessibilityNotes());
    assertThat(unit.get("created_at", LocalDateTime.class)).isEqualTo(expected.createdAt());
    assertThat(unit.get("updated_at", LocalDateTime.class)).isEqualTo(expected.updatedAt());
    assertThat(unit.get("created_by", UUID.class)).isEqualTo(expected.createdBy());
    assertThat(unit.get("updated_by", UUID.class)).isEqualTo(expected.updatedBy());
    assertThat(unit.get("deleted_at", LocalDateTime.class)).isNull();
  }

  private void assertResidentialDetailsCopiedOnto(
      UUID propertyId, UUID teamId, ResidentialDetails expected) {
    Record details =
        Objects.requireNonNull(
            dsl.select(DSL.asterisk())
                .from(DSL.table("unit_residential_details"))
                .where(DSL.field("unit_id").eq(implicitUnitId(propertyId)))
                .fetchOne(),
            "expected exactly one copied unit_residential_details row for property " + propertyId);

    assertThat(details.get("team_id", UUID.class)).isEqualTo(teamId);
    assertThat(details.get("bedrooms", Integer.class)).isEqualTo(expected.bedrooms());
    assertThat(details.get("bathrooms", Integer.class)).isEqualTo(expected.bathrooms());
    assertThat(details.get("furnished", Boolean.class)).isEqualTo(expected.furnished());
    assertThat(details.get("pet_policy", String.class)).isEqualTo(expected.petPolicy());
    assertThat(details.get("created_at", LocalDateTime.class)).isEqualTo(expected.createdAt());
    assertThat(details.get("updated_at", LocalDateTime.class)).isEqualTo(expected.updatedAt());
    assertThat(details.get("created_by", UUID.class)).isEqualTo(expected.createdBy());
    assertThat(details.get("updated_by", UUID.class)).isEqualTo(expected.updatedBy());
  }

  private void assertAmenityLinkCopiedOnto(UUID propertyId, UUID teamId, AmenityLink expected) {
    Record link =
        Objects.requireNonNull(
            dsl.select(DSL.asterisk())
                .from(DSL.table("unit_amenities"))
                .where(DSL.field("unit_id").eq(implicitUnitId(propertyId)))
                .fetchOne(),
            "expected exactly one copied unit_amenities row for property " + propertyId);

    assertThat(link.get("team_id", UUID.class)).isEqualTo(teamId);
    assertThat(link.get("amenity_id", UUID.class)).isEqualTo(amenityId(expected.amenityName()));
    assertThat(link.get("notes", String.class)).isEqualTo(expected.notes());
    assertThat(link.get("created_at", LocalDateTime.class)).isEqualTo(expected.createdAt());
    assertThat(link.get("updated_at", LocalDateTime.class)).isEqualTo(expected.updatedAt());
    assertThat(link.get("created_by", UUID.class)).isEqualTo(expected.createdBy());
    assertThat(link.get("updated_by", UUID.class)).isEqualTo(expected.updatedBy());
    assertThat(link.get("deleted_at", LocalDateTime.class)).isEqualTo(expected.deletedAt());
  }

  private void assertUnitMatchesOwnProperty(
      UUID propertyId, String status, BigDecimal areaValue, String energyRating) {
    var unit =
        Objects.requireNonNull(
            dsl.select(
                    DSL.field("status", String.class),
                    DSL.field("area_value", BigDecimal.class),
                    DSL.field("energy_efficiency_rating", String.class))
                .from(DSL.table("units"))
                .where(DSL.field("property_id").eq(propertyId))
                .fetchOne(),
            "expected exactly one backfilled unit for property " + propertyId);

    assertThat(unit.value1()).isEqualTo(status);
    assertThat(unit.value2()).isEqualByComparingTo(areaValue);
    assertThat(unit.value3()).isEqualTo(energyRating);
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

  /** Seeds a property with a value in every column V070's units backfill reads. */
  private UUID seedDwellingProperty(UUID teamId, String street, PropertyDwelling dwelling) {
    seedTeamAndUser(teamId);
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("properties"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "PRO" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_category", String.class), dwelling.propertyCategory())
        .set(DSL.field("property_type", String.class), dwelling.propertyType())
        .set(DSL.field("status", String.class), dwelling.status())
        .set(DSL.field("street", String.class), street)
        .set(DSL.field("city", String.class), "Amsterdam")
        .set(DSL.field("postal_code", String.class), "1015 CJ")
        .set(DSL.field("country_code", String.class), "NL")
        .set(DSL.field("area_value", BigDecimal.class), dwelling.areaValue())
        .set(DSL.field("area_unit", String.class), dwelling.areaUnit())
        .set(DSL.field("energy_efficiency_rating", String.class), dwelling.energyEfficiencyRating())
        .set(
            DSL.field("energy_certificate_expiry_date", LocalDate.class),
            dwelling.energyCertificateExpiryDate())
        .set(DSL.field("heating_type", String.class), dwelling.heatingType())
        .set(DSL.field("cooling_type", String.class), dwelling.coolingType())
        .set(DSL.field("hot_water_system", String.class), dwelling.hotWaterSystem())
        .set(DSL.field("insulation_notes", String.class), dwelling.insulationNotes())
        .set(DSL.field("flooring_type", String.class), dwelling.flooringType())
        .set(DSL.field("window_type", String.class), dwelling.windowType())
        .set(DSL.field("has_smoke_detectors", Boolean.class), dwelling.hasSmokeDetectors())
        .set(DSL.field("has_co_detectors", Boolean.class), dwelling.hasCoDetectors())
        .set(DSL.field("has_fire_extinguisher", Boolean.class), dwelling.hasFireExtinguisher())
        .set(DSL.field("has_adapted_bathroom", Boolean.class), dwelling.hasAdaptedBathroom())
        .set(DSL.field("accessibility_notes", String.class), dwelling.accessibilityNotes())
        .set(DSL.field("created_at", LocalDateTime.class), dwelling.createdAt())
        .set(DSL.field("updated_at", LocalDateTime.class), dwelling.updatedAt())
        .set(DSL.field("created_by", UUID.class), dwelling.createdBy())
        .set(DSL.field("updated_by", UUID.class), dwelling.updatedBy())
        .execute();
    return id;
  }

  private void seedResidentialDetails(UUID teamId, UUID propertyId, ResidentialDetails details) {
    dsl.insertInto(DSL.table("property_residential_details"))
        .set(DSL.field("id", UUID.class), UUID.randomUUID())
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("bedrooms", Integer.class), details.bedrooms())
        .set(DSL.field("bathrooms", Integer.class), details.bathrooms())
        .set(DSL.field("furnished", Boolean.class), details.furnished())
        .set(DSL.field("pet_policy", String.class), details.petPolicy())
        .set(DSL.field("created_at", LocalDateTime.class), details.createdAt())
        .set(DSL.field("updated_at", LocalDateTime.class), details.updatedAt())
        .set(DSL.field("created_by", UUID.class), details.createdBy())
        .set(DSL.field("updated_by", UUID.class), details.updatedBy())
        .execute();
  }

  private void seedAmenityLink(UUID teamId, UUID propertyId, AmenityLink link) {
    dsl.insertInto(DSL.table("property_amenities"))
        .set(DSL.field("id", UUID.class), UUID.randomUUID())
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("amenity_id", UUID.class), amenityId(link.amenityName()))
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("notes", String.class), link.notes())
        .set(DSL.field("created_at", LocalDateTime.class), link.createdAt())
        .set(DSL.field("updated_at", LocalDateTime.class), link.updatedAt())
        .set(DSL.field("created_by", UUID.class), link.createdBy())
        .set(DSL.field("updated_by", UUID.class), link.updatedBy())
        .set(DSL.field("deleted_at", LocalDateTime.class), link.deletedAt())
        .execute();
  }

  private UUID amenityId(String name) {
    return Objects.requireNonNull(
        dsl.select(DSL.field("id", UUID.class))
            .from(DSL.table("amenities"))
            .where(DSL.field("name").eq(name))
            .fetchOne(0, UUID.class),
        "seeded amenity " + name + " not found");
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

  private UUID seedOccupancyPeriod(UUID teamId, UUID propertyId) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("property_occupancy_periods"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "OCP" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("start_date", LocalDate.class), LocalDate.of(2026, 1, 1))
        .set(DSL.field("type", String.class), "PRIMARY_RESIDENCE")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), USER_ID)
        .set(DSL.field("updated_by", UUID.class), USER_ID)
        .execute();
    return id;
  }

  private UUID seedWwsCalculation(UUID teamId, UUID propertyId) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("wws_calculations"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), "WWS" + randomSuffix())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("system_version", String.class), "2026")
        .set(DSL.field("total_points", BigDecimal.class), new BigDecimal("142.50"))
        .set(DSL.field("sector_classification", String.class), "LIBERALIZED")
        .set(DSL.field("category_breakdown", JSONB.class), JSONB.jsonb("{}"))
        .set(DSL.field("input_data", JSONB.class), JSONB.jsonb("{}"))
        .set(DSL.field("calculation_date", LocalDate.class), LocalDate.of(2026, 1, 1))
        .execute();
    return id;
  }

  private void seedOtherUser() {
    dsl.insertInto(DSL.table("users"))
        .set(DSL.field("id", UUID.class), OTHER_USER_ID)
        .set(DSL.field("identifier", String.class), "USR" + randomSuffix())
        .set(DSL.field("keycloak_id", String.class), "kc-" + OTHER_USER_ID)
        .set(
            DSL.field("email", String.class), OTHER_USER_ID.toString().substring(0, 8) + "@test.io")
        .set(DSL.field("first_name", String.class), "Other")
        .set(DSL.field("last_name", String.class), "User")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .onConflictDoNothing()
        .execute();
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
    return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase(Locale.ROOT);
  }
}
