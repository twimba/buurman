# Units Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a `Unit` entity under `Property` in the database and backend API, with every existing property backfilled to one hidden implicit unit and a stored per-unit cost allocation engine.

**Architecture:** `properties` becomes the building (address, structure, financing); a new `units` table becomes the dwelling (area, rooms, energy label, status). Dwelling columns move off `properties` onto `units`. Every property always has at least one unit, so `unit_id` is `NOT NULL` on contracts, occupancy periods and WWS calculations and no aggregation ever branches on unit presence. Building-level expenses split into stored `expense_allocations` rows using largest-remainder rounding.

**Tech Stack:** Java 25, Spring Boot 4.0.2, JOOQ 3.20, Flyway, PostgreSQL 18, MapStruct, Lombok, JUnit 5, Mockito, AssertJ, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-09-26-units-multi-unit-buildings-design.md`

**Plan series:** This is plan 1 of 3. Plan 2 covers the frontend; plan 3 covers exports, booklets, takeout, letters, demo data and i18n. This plan ships a working, tested backend on its own.

## Global Constraints

- Every repository query filters by `team_id` in its WHERE clause. No exceptions.
- Never expose internal UUIDs in API responses — only `identifier` (Sid).
- Soft delete via `deleted_at`. Never hard delete.
- Set `created_by` / `updated_by` manually in every INSERT and UPDATE.
- All `if`, `else`, `for`, `while` bodies use curly braces. No brace-less bodies, ever.
- Use idiomatic `Optional` (`map`, `orElse`, `orElseThrow`, `ifPresent`, `flatMap`). Never `opt.get()` without an `isPresent()` check, never `if (opt != null)`.
- Domain POJOs use `@Data @Builder @NoArgsConstructor @AllArgsConstructor` with `@Builder.Default private Optional<X> field = Optional.empty();` for nullable fields.
- Google Java Style. Conventional commits (`feat:`, `fix:`, `docs:`, `chore:`).
- Flyway migrations are append-only. **Never modify an existing migration.** Next version is **V070**.
- Sid identifiers are `VARCHAR(29)`: a 3-char `EntityPrefix` plus 26 ULID chars.
- `UnitStatus` values are exactly: `VACANT`, `OCCUPIED`, `SELF_OCCUPIED`, `MAINTENANCE`, `UNAVAILABLE`, `UNDER_RENOVATION`, `FALLOW`, `LISTED`.
- `UnitType` values are exactly: `APARTMENT`, `PARKING`, `STORAGE`, `COMMERCIAL`. ROOM is deliberately out of scope.
- `AllocationBasis` values are exactly: `AREA`, `EQUAL`, `CUSTOM`, `MANUAL`.
- Money is stored in minor units (`BIGINT`) plus a `VARCHAR(3)` currency column, wrapped by `MoneyAmount`.
- **Typed identifiers extend `Sid` and have private constructors.** Build them with the static factory — `UnitIdentifier.of("UNT...")`, `Sid.of(...)` — never `new UnitIdentifier(...)`. Because they *are* `Sid`s, pass them straight to a repository method taking `Sid`; there is no `.toSid()`.

## Review Focus

Five failure modes the spec implies but that no task's happy-path tests would exercise. Each has its test assigned to the task that owns the code.

1. **`AREA` basis when every sibling unit has `NULL` area** — a naive share computation divides by zero. Expected: fall back to `EQUAL` and record `EQUAL` on each row's `basis`. → Task 10.
2. **Units belonging to a soft-deleted property** — the backfill deliberately creates implicit units for soft-deleted properties to preserve FK targets, so a team-scoped unit listing that forgets to join on `properties.deleted_at IS NULL` silently inflates `billableUnitCount` and occupancy. Expected: excluded from listings and counts. → Task 5 and Task 15.
3. **Duplicate `unit_number` within one property** — the partial unique index raises a constraint violation. Expected: a `409` business-rule error naming the duplicate, never a raw 500. → Task 7.
4. **A negative expense amount** (credit note, refund) under any basis — largest-remainder rounding must still sum exactly to the negative total rather than distributing a stray cent the wrong way. → Task 10.
5. **Cross-currency allocation** — a building expense in EUR against a property whose units carry no currency of their own. Expected: every allocation row inherits the expense's currency; allocation never mixes currencies or infers one. → Task 10.

---

## File Structure

**Migrations**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V070__units.sql` — the entire model change in one transactional migration.

**`buurman-common`**
- Create: `domain/Unit.java` — dwelling POJO, Optional-based.
- Create: `domain/UnitType.java`, `domain/UnitStatus.java`, `domain/AllocationBasis.java` — enums.
- Create: `domain/ExpenseAllocation.java` — allocation row POJO.
- Create: `domain/identifier/UnitIdentifier.java`, `domain/identifier/ExpenseAllocationIdentifier.java`.
- Create: `dto/request/CreateUnitRequest.java`, `UpdateUnitRequest.java`, `BulkCreateUnitsRequest.java`, `UpdateAllocationRequest.java`, `ManualAllocationRequest.java`.
- Create: `dto/response/UnitResponse.java`, `UnitGridRowResponse.java`, `UnitSummaryResponse.java`, `ExpenseAllocationResponse.java`.
- Modify: `util/EntityPrefix.java` — add `UNT`, `EAL`.
- Modify: `util/SidGenerator.java` — add `newUnitId()`, `newExpenseAllocationId()`.
- Modify: `domain/Property.java` — remove dwelling fields and the `PropertyStatus` enum.
- Modify: `dto/response/PropertyResponse.java` — remove dwelling fields, add unit counts.
- Modify: `dto/request/CreatePropertyRequest.java`, `UpdatePropertyRequest.java` — remove dwelling fields, add inline `unit`.

**`buurman-core`**
- Create: `repository/UnitRepository.java`, `repository/ExpenseAllocationRepository.java`.
- Create: `mapper/UnitRecordMapper.java`, `mapper/UnitMapper.java`, `mapper/ExpenseAllocationRecordMapper.java`.
- Create: `service/UnitService.java`, `service/ExpenseAllocationService.java`.
- Create: `controller/UnitController.java`.
- Modify: `repository/PropertyRepository.java`, `mapper/PropertyRecordMapper.java`, `service/PropertyService.java`, `service/ContractService.java`, `service/OccupancyPeriodService.java`, WWS services, `service/TeamService.java`.

**Tests**
- Create: `buurman-core/src/test/java/com/buurman/repository/AbstractMigrationIntegrationTest.java` — Flyway-to-V067 base, distinct from `AbstractRepositoryIntegrationTest`.
- Create: `repository/UnitBackfillMigrationIntegrationTest.java`, `repository/UnitRepositoryIntegrationTest.java`.
- Create: `service/UnitServiceTest.java`, `service/ExpenseAllocationServiceTest.java`.
- Modify: `repository/AbstractRepositoryIntegrationTest.java` — truncation list gains `expense_allocations`, `unit_amenities`, `unit_residential_details`, `units`; loses `property_amenities`.

**OpenAPI**
- Create: `openapi/src/paths/units.yaml`.
- Modify: `openapi/src/app.yaml`, `openapi/src/paths/properties.yaml`, `contracts.yaml`, `expenses.yaml`, `teams.yaml`.

---

## Task 1: Migration test harness and the failing backfill test

`AbstractRepositoryIntegrationTest` applies **all** migrations at container start, so it cannot seed pre-V070 state. This task builds a separate base class that migrates to V067, lets the test seed, then migrates the rest of the way.

**Files:**
- Create: `backend/buurman-core/src/test/java/com/buurman/repository/AbstractMigrationIntegrationTest.java`
- Create: `backend/buurman-core/src/test/java/com/buurman/repository/UnitBackfillMigrationIntegrationTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `AbstractMigrationIntegrationTest` with `protected static DSLContext dsl`, `protected void migrateTo(String version)`, `protected static final UUID TEAM_A_ID`, `TEAM_B_ID`, `USER_ID`. Used by no later task, but the pattern is reused in plan 3.

- [ ] **Step 1: Write the migration base class**

Create `AbstractMigrationIntegrationTest.java`:

```java
package com.buurman.repository;

import java.util.UUID;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for tests that must observe a migration's effect on pre-existing data. Unlike {@link
 * AbstractRepositoryIntegrationTest}, this class does NOT run all migrations up front: it migrates
 * to a chosen baseline, lets the test seed rows, then migrates further. Each test gets a freshly
 * dropped-and-recreated schema.
 */
abstract class AbstractMigrationIntegrationTest {

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>("postgres:18-alpine")
          .withDatabaseName("buurman_migration_test")
          .withUsername("buurman")
          .withPassword("buurman");

  private static PGSimpleDataSource dataSource;
  protected static DSLContext dsl;

  protected static final UUID TEAM_A_ID = UUID.randomUUID();
  protected static final UUID TEAM_B_ID = UUID.randomUUID();
  protected static final UUID USER_ID = UUID.randomUUID();

  static {
    PG.start();
    dataSource = new PGSimpleDataSource();
    dataSource.setUrl(PG.getJdbcUrl());
    dataSource.setUser(PG.getUsername());
    dataSource.setPassword(PG.getPassword());
    dsl = DSL.using((DataSource) dataSource, SQLDialect.POSTGRES);
  }

  @BeforeEach
  void resetSchema() {
    dsl.execute("DROP SCHEMA public CASCADE");
    dsl.execute("CREATE SCHEMA public");
  }

  /** Migrates the schema up to and including {@code version}, e.g. "067". */
  protected void migrateTo(String version) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .target(version)
        .cleanDisabled(true)
        .load()
        .migrate();
  }

  /** Migrates every remaining migration. */
  protected void migrateToLatest() {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
  }
}
```

- [ ] **Step 2: Write the failing backfill test**

Create `UnitBackfillMigrationIntegrationTest.java`. This seeds a property with dwelling data, a contract, an occupancy period and a WWS calculation at V067, then migrates to V070 and asserts the backfill.

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V070 units backfill")
class UnitBackfillMigrationIntegrationTest extends AbstractMigrationIntegrationTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

  @Test
  @DisplayName("gives every property exactly one implicit unit carrying its dwelling data")
  void backfillsImplicitUnitPerProperty() {
    migrateTo("067");
    UUID propertyId = seedProperty(TEAM_A_ID, "Keizersgracht 12", "OCCUPIED", new BigDecimal("85.50"), "B");

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
    UUID propertyId = seedProperty(TEAM_A_ID, "Prinsengracht 4", "OCCUPIED", new BigDecimal("70"), "C");
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
        .set(DSL.field("start_date", LocalDate.class), LocalDate.of(2026, 1, 1))
        .set(DSL.field("status", String.class), "ACTIVE")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
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
```

**Note for the implementer:** the seeding helpers reference `country_code` (not `country`) because V028 standardised country codes, and there is no `contract_number` column. If a seed INSERT fails on a missing or extra column, read the V067-era schema for that table rather than guessing — `V004__contracts_and_payments.sql` for contracts, `V002__properties.sql` plus later `ALTER TABLE properties` statements for properties.

- [ ] **Step 3: Run the test to verify it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitBackfillMigrationIntegrationTest'`

Expected: FAIL. Every test errors with a PostgreSQL error like `relation "units" does not exist`, because V070 has not been written yet. Docker must be running.

- [ ] **Step 4: Commit the harness**

```bash
git add backend/buurman-core/src/test/java/com/buurman/repository/AbstractMigrationIntegrationTest.java \
        backend/buurman-core/src/test/java/com/buurman/repository/UnitBackfillMigrationIntegrationTest.java
git commit -m "test: add migration harness and failing units backfill test (BUUR-106)"
```

---

## Task 2: The V070 migration

Makes Task 1's test pass. **After this task the main source set will not compile** — dropping `properties.status` and the dwelling columns invalidates JOOQ-generated references in `Property`, `PropertyRecordMapper` and `PropertyRepository`. Task 4 restores compilation. This is expected and is why Task 3 and Task 4 follow immediately.

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V070__units.sql`

**Interfaces:**
- Consumes: `AbstractMigrationIntegrationTest` from Task 1.
- Produces: tables `units`, `unit_residential_details`, `unit_amenities`, `expense_allocations`; `properties.allocation_basis`; `unit_id` on `contracts`, `property_occupancy_periods`, `wws_calculations` (NOT NULL) and on `photos`, `documents`, `expenses` (nullable). JOOQ generates `Tables.UNITS`, `Tables.UNIT_RESIDENTIAL_DETAILS`, `Tables.UNIT_AMENITIES`, `Tables.EXPENSE_ALLOCATIONS`.

- [ ] **Step 1: Write the migration — table creation**

Create `V070__units.sql` starting with:

```sql
-- =============================================================================
-- V070__units.sql
-- Introduce a Unit entity under Property (BUUR-106).
-- Property becomes the building; Unit becomes the dwelling.
-- Every existing property is backfilled with one implicit unit, so unit_id can
-- be NOT NULL on contracts, occupancy periods and WWS calculations.
-- =============================================================================
-- 1. units
-- =============================================================================
CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties (id),
    -- Identity within the building
    name VARCHAR(255),
    unit_number VARCHAR(50) NOT NULL,
    floor INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    unit_type VARCHAR(30) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VACANT',
    is_implicit BOOLEAN NOT NULL DEFAULT FALSE,
    -- Valuation & allocation shares
    woz_value BIGINT,
    woz_value_currency VARCHAR(3),
    woz_share_pct NUMERIC(6, 3),
    allocation_share NUMERIC(6, 3),
    -- Dwelling: specifications
    area_value NUMERIC(10, 2),
    area_unit VARCHAR(10) NOT NULL DEFAULT 'sqm',
    -- Dwelling: energy & climate
    energy_efficiency_rating VARCHAR(5),
    energy_certificate_expiry_date DATE,
    heating_type VARCHAR(50),
    cooling_type VARCHAR(50),
    hot_water_system VARCHAR(50),
    insulation_notes TEXT,
    -- Dwelling: finishes
    flooring_type VARCHAR(50),
    window_type VARCHAR(50),
    -- Dwelling: safety
    has_smoke_detectors BOOLEAN DEFAULT FALSE,
    has_co_detectors BOOLEAN DEFAULT FALSE,
    has_fire_extinguisher BOOLEAN DEFAULT FALSE,
    -- Dwelling: accessibility
    has_adapted_bathroom BOOLEAN DEFAULT FALSE,
    accessibility_notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_units_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_units_type CHECK (
        unit_type IN ('APARTMENT', 'PARKING', 'STORAGE', 'COMMERCIAL')
    ),
    CONSTRAINT chk_units_area_positive CHECK (
        area_value IS NULL
        OR area_value > 0
    ),
    CONSTRAINT chk_units_woz_share CHECK (
        woz_share_pct IS NULL
        OR (woz_share_pct >= 0 AND woz_share_pct <= 100)
    ),
    CONSTRAINT chk_units_allocation_share CHECK (
        allocation_share IS NULL
        OR (allocation_share >= 0 AND allocation_share <= 100)
    )
);

CREATE UNIQUE INDEX uq_units_property_number ON units (property_id, unit_number)
WHERE
    (deleted_at IS NULL);

CREATE INDEX idx_units_team_id ON units (team_id);

CREATE INDEX idx_units_property_id ON units (property_id);

CREATE INDEX idx_units_status ON units (team_id, status)
WHERE
    (deleted_at IS NULL);

-- =============================================================================
-- 2. unit_residential_details
-- =============================================================================
CREATE TABLE unit_residential_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES units (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    bedrooms INTEGER,
    bathrooms INTEGER,
    furnished BOOLEAN NOT NULL DEFAULT FALSE,
    pet_policy VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uq_unit_residential_details_unit UNIQUE (unit_id, team_id),
    CONSTRAINT chk_unit_residential_bedrooms CHECK (bedrooms >= 0),
    CONSTRAINT chk_unit_residential_bathrooms CHECK (bathrooms >= 0)
);

-- =============================================================================
-- 3. unit_amenities
-- =============================================================================
CREATE TABLE unit_amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES units (id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES amenities (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_unit_amenities UNIQUE (unit_id, amenity_id, team_id)
);

-- =============================================================================
-- 4. expense_allocations
-- =============================================================================
CREATE TABLE expense_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    expense_id UUID NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES units (id),
    amount BIGINT NOT NULL,
    amount_currency VARCHAR(3) NOT NULL,
    basis VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_expense_allocations_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_expense_allocations_basis CHECK (
        basis IN ('AREA', 'EQUAL', 'CUSTOM', 'MANUAL')
    )
);

CREATE UNIQUE INDEX uq_expense_allocations_expense_unit ON expense_allocations (expense_id, unit_id)
WHERE
    (deleted_at IS NULL);

CREATE INDEX idx_expense_allocations_unit_id ON expense_allocations (unit_id);

CREATE INDEX idx_expense_allocations_expense_id ON expense_allocations (expense_id);

-- =============================================================================
-- 5. properties.allocation_basis
-- =============================================================================
ALTER TABLE properties
ADD COLUMN allocation_basis VARCHAR(20) NOT NULL DEFAULT 'EQUAL';

ALTER TABLE properties
ADD CONSTRAINT chk_properties_allocation_basis CHECK (
    allocation_basis IN ('AREA', 'EQUAL', 'CUSTOM', 'MANUAL')
);
```

- [ ] **Step 2: Append the backfill**

Continue in the same file. Note `properties p` is **not** filtered on `deleted_at` — soft-deleted properties need implicit units so their contracts keep a valid FK target.

```sql
-- =============================================================================
-- 6. Backfill one implicit unit per property (soft-deleted ones included, so
--    their contracts keep a valid FK target).
-- =============================================================================
INSERT INTO
    units (
        identifier,
        team_id,
        property_id,
        unit_number,
        sort_order,
        unit_type,
        status,
        is_implicit,
        allocation_share,
        area_value,
        area_unit,
        energy_efficiency_rating,
        energy_certificate_expiry_date,
        heating_type,
        cooling_type,
        hot_water_system,
        insulation_notes,
        flooring_type,
        window_type,
        has_smoke_detectors,
        has_co_detectors,
        has_fire_extinguisher,
        has_adapted_bathroom,
        accessibility_notes,
        created_at,
        updated_at,
        created_by,
        updated_by,
        deleted_at
    )
SELECT
    'UNT' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.team_id,
    p.id,
    '1',
    0,
    CASE p.property_category
        WHEN 'COMMERCIAL' THEN 'COMMERCIAL'
        ELSE 'APARTMENT'
    END,
    p.status,
    TRUE,
    100,
    p.area_value,
    coalesce(p.area_unit, 'sqm'),
    p.energy_efficiency_rating,
    p.energy_certificate_expiry_date,
    p.heating_type,
    p.cooling_type,
    p.hot_water_system,
    p.insulation_notes,
    p.flooring_type,
    p.window_type,
    p.has_smoke_detectors,
    p.has_co_detectors,
    p.has_fire_extinguisher,
    p.has_adapted_bathroom,
    p.accessibility_notes,
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by,
    p.deleted_at
FROM
    properties p;

-- Copy residential details onto the implicit unit
INSERT INTO
    unit_residential_details (
        unit_id,
        team_id,
        bedrooms,
        bathrooms,
        furnished,
        pet_policy,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    u.id,
    d.team_id,
    d.bedrooms,
    d.bathrooms,
    d.furnished,
    d.pet_policy,
    d.created_at,
    d.updated_at,
    d.created_by,
    d.updated_by
FROM
    property_residential_details d
    JOIN units u ON u.property_id = d.property_id
    AND u.is_implicit;

-- Copy amenity links onto the implicit unit
INSERT INTO
    unit_amenities (
        unit_id,
        amenity_id,
        team_id,
        notes,
        created_at,
        updated_at,
        created_by,
        updated_by,
        deleted_at
    )
SELECT
    u.id,
    a.amenity_id,
    a.team_id,
    a.notes,
    a.created_at,
    a.updated_at,
    a.created_by,
    a.updated_by,
    a.deleted_at
FROM
    property_amenities a
    JOIN units u ON u.property_id = a.property_id
    AND u.is_implicit;
```

- [ ] **Step 3: Append the `unit_id` wiring**

```sql
-- =============================================================================
-- 7. unit_id on contracts, occupancy periods and WWS calculations (NOT NULL)
-- =============================================================================
ALTER TABLE contracts
ADD COLUMN unit_id UUID REFERENCES units (id);

UPDATE contracts c
SET
    unit_id = u.id
FROM
    units u
WHERE
    u.property_id = c.property_id
    AND u.is_implicit;

ALTER TABLE contracts
ALTER COLUMN unit_id
SET NOT NULL;

CREATE INDEX idx_contracts_unit_id ON contracts (unit_id);

ALTER TABLE property_occupancy_periods
ADD COLUMN unit_id UUID REFERENCES units (id);

UPDATE property_occupancy_periods o
SET
    unit_id = u.id
FROM
    units u
WHERE
    u.property_id = o.property_id
    AND u.is_implicit;

ALTER TABLE property_occupancy_periods
ALTER COLUMN unit_id
SET NOT NULL;

CREATE INDEX idx_occupancy_periods_unit_id ON property_occupancy_periods (unit_id);

ALTER TABLE wws_calculations
ADD COLUMN unit_id UUID REFERENCES units (id);

UPDATE wws_calculations w
SET
    unit_id = u.id
FROM
    units u
WHERE
    u.property_id = w.property_id
    AND u.is_implicit;

ALTER TABLE wws_calculations
ALTER COLUMN unit_id
SET NOT NULL;

CREATE INDEX idx_wws_calculations_unit_id ON wws_calculations (unit_id);

-- =============================================================================
-- 8. Optional unit_id on photos, documents and expenses
-- =============================================================================
ALTER TABLE photos
ADD COLUMN unit_id UUID REFERENCES units (id);

CREATE INDEX idx_photos_unit_id ON photos (unit_id);

ALTER TABLE documents
ADD COLUMN unit_id UUID REFERENCES units (id);

CREATE INDEX idx_documents_unit_id ON documents (unit_id);

ALTER TABLE expenses
ADD COLUMN unit_id UUID REFERENCES units (id);

CREATE INDEX idx_expenses_unit_id ON expenses (unit_id);

-- =============================================================================
-- 9. Re-scope the occupancy no-overlap exclusion from property to unit
-- =============================================================================
ALTER TABLE property_occupancy_periods
DROP CONSTRAINT excl_occupancy_periods_no_overlap;

ALTER TABLE property_occupancy_periods
ADD CONSTRAINT excl_occupancy_periods_no_overlap EXCLUDE USING gist (
    unit_id
    WITH
        =,
        daterange (
            start_date,
            coalesce(end_date, '9999-12-31'::date),
            '[]'
        )
    WITH
        &&
)
WHERE
    (deleted_at IS NULL);
```

- [ ] **Step 4: Append the drops**

```sql
-- =============================================================================
-- 10. Drop what has moved. Property is now building-only.
-- =============================================================================
DROP TABLE property_amenities;

DROP TABLE property_residential_details;

ALTER TABLE properties
DROP COLUMN status,
DROP COLUMN area_value,
DROP COLUMN area_unit,
DROP COLUMN energy_efficiency_rating,
DROP COLUMN energy_certificate_expiry_date,
DROP COLUMN heating_type,
DROP COLUMN cooling_type,
DROP COLUMN hot_water_system,
DROP COLUMN insulation_notes,
DROP COLUMN flooring_type,
DROP COLUMN window_type,
DROP COLUMN has_smoke_detectors,
DROP COLUMN has_co_detectors,
DROP COLUMN has_fire_extinguisher,
DROP COLUMN has_adapted_bathroom,
DROP COLUMN accessibility_notes;
```

**Note for the implementer:** if `DROP COLUMN status` fails on a dependent index or CHECK constraint, drop that object first in the same statement block. Run `\d properties` against a migrated dev database to list dependents. Do not use `CASCADE` blindly — read what it would remove first.

- [ ] **Step 5: Run the backfill test to verify it passes**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitBackfillMigrationIntegrationTest'`

Expected: PASS, all four tests.

- [ ] **Step 6: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am`

Expected: success. `backend/buurman-jooq/target/generated-sources/jooq/com/buurman/jooq/generated/tables/Units.java` now exists.

- [ ] **Step 7: Confirm the expected compile break**

Run: `cd backend && mvn compile -pl buurman-core -am -Pquick`

Expected: **FAIL** with `cannot find symbol` on `PROPERTIES.STATUS`, `PROPERTIES.AREA_VALUE` and friends in `PropertyRepository` and `PropertyRecordMapper`. This is the expected state; Task 4 fixes it. Record the full list of failing symbols — it is the checklist for Task 4.

- [ ] **Step 8: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V070__units.sql
git commit -m "feat(db): add units, unit details, amenities and expense allocations (BUUR-106)

Backfills one implicit unit per property (soft-deleted included) so unit_id
can be NOT NULL on contracts, occupancy periods and WWS calculations.
Drops the dwelling columns from properties; property is now building-only.

Main source set does not compile until the Property domain refactor lands."
```

---

## Task 3: Unit domain types, enums and identifiers

Pure `buurman-common` additions. Compiles independently of the broken core module.

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/UnitType.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/UnitStatus.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/AllocationBasis.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/Unit.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/ExpenseAllocation.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/identifier/UnitIdentifier.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/identifier/ExpenseAllocationIdentifier.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/SidGenerator.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `Unit` (Lombok builder), `ExpenseAllocation`, `UnitType`, `UnitStatus`, `AllocationBasis`, `UnitIdentifier`, `ExpenseAllocationIdentifier`, `EntityPrefix.UNT`, `EntityPrefix.EAL`, `SidGenerator.newUnitId()`, `SidGenerator.newExpenseAllocationId()`. Every later task in this plan depends on these names.

- [ ] **Step 1: Write the enums**

`UnitType.java`:

```java
package com.buurman.domain;

/**
 * Kind of separately-lettable object within a property. ROOM is deliberately absent: room rentals
 * and HMO cost splitting are a separate follow-up issue (see the BUUR-106 spec, Out of scope).
 */
public enum UnitType {
  APARTMENT,
  PARKING,
  STORAGE,
  COMMERCIAL
}
```

`UnitStatus.java` — a verbatim move of the former `Property.PropertyStatus`, so the V070 backfill copies values across with no mapping:

```java
package com.buurman.domain;

/** Occupancy state of a single unit. Moved verbatim from the former Property.PropertyStatus. */
public enum UnitStatus {
  VACANT,
  OCCUPIED,
  SELF_OCCUPIED,
  MAINTENANCE,
  UNAVAILABLE,
  UNDER_RENOVATION,
  FALLOW,
  LISTED
}
```

`AllocationBasis.java`:

```java
package com.buurman.domain;

/** How a building-level expense is divided across a property's units. */
public enum AllocationBasis {
  /** Proportional to each unit's area. Falls back to EQUAL when no unit has an area. */
  AREA,
  /** Equal split across all units. */
  EQUAL,
  /** Proportional to each unit's allocation_share percentage. */
  CUSTOM,
  /** Caller-supplied per-unit amounts. */
  MANUAL
}
```

- [ ] **Step 2: Write the Unit domain POJO**

`Unit.java`. Follows the `Property` convention exactly: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@SuppressWarnings("NullAway.Init")`, and `@Builder.Default private Optional<X> ... = Optional.empty()` for every nullable field.

```java
package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A separately-lettable dwelling within a {@link Property}. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID propertyId;

  // Identity within the building
  @Builder.Default private Optional<String> name = Optional.empty();
  private String unitNumber;
  @Builder.Default private Optional<Integer> floor = Optional.empty();
  @Builder.Default private int sortOrder = 0;
  private UnitType unitType;
  private UnitStatus status;

  /**
   * True while this unit is the auto-created stand-in for a single-unit property. A UI-visibility
   * hint only — never a filter in an aggregation.
   */
  @Builder.Default private boolean implicit = false;

  // Valuation & allocation shares
  @Builder.Default private Optional<BigDecimal> wozValue = Optional.empty();
  @Builder.Default private Optional<String> wozValueCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> wozSharePct = Optional.empty();
  @Builder.Default private Optional<BigDecimal> allocationShare = Optional.empty();

  // Specifications
  @Builder.Default private Optional<BigDecimal> areaValue = Optional.empty();
  @Builder.Default private Optional<String> areaUnit = Optional.empty();

  // Energy & climate
  @Builder.Default private Optional<String> energyEfficiencyRating = Optional.empty();
  @Builder.Default private Optional<LocalDate> energyCertificateExpiryDate = Optional.empty();
  @Builder.Default private Optional<String> heatingType = Optional.empty();
  @Builder.Default private Optional<String> coolingType = Optional.empty();
  @Builder.Default private Optional<String> hotWaterSystem = Optional.empty();
  @Builder.Default private Optional<String> insulationNotes = Optional.empty();

  // Finishes
  @Builder.Default private Optional<String> flooringType = Optional.empty();
  @Builder.Default private Optional<String> windowType = Optional.empty();

  // Safety
  @Builder.Default private Optional<Boolean> hasSmokeDetectors = Optional.empty();
  @Builder.Default private Optional<Boolean> hasCoDetectors = Optional.empty();
  @Builder.Default private Optional<Boolean> hasFireExtinguisher = Optional.empty();

  // Accessibility
  @Builder.Default private Optional<Boolean> hasAdaptedBathroom = Optional.empty();
  @Builder.Default private Optional<String> accessibilityNotes = Optional.empty();

  // Audit
  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

- [ ] **Step 3: Write the ExpenseAllocation domain POJO**

```java
package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One unit's stored share of a building-level expense. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAllocation {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID expenseId;
  private UUID unitId;
  private MoneyAmount amount;
  private AllocationBasis basis;

  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

- [ ] **Step 4: Write the typed identifiers**

Open `backend/buurman-common/src/main/java/com/buurman/domain/identifier/PropertyIdentifier.java` and copy its exact shape. Create `UnitIdentifier.java` and `ExpenseAllocationIdentifier.java` following it, substituting `EntityPrefix.UNT` and `EntityPrefix.EAL`. Do not invent a different structure — match the existing file so `SidJooqConverter` keeps working.

- [ ] **Step 5: Register the prefixes**

In `EntityPrefix.java`, add two entries in the existing alphabetical-ish style:

```java
  UNT("UNT", "Units"),
  EAL("EAL", "Expense Allocations"),
```

In `SidGenerator.java`, add the two factory methods next to the existing ones, importing `UnitIdentifier` and `ExpenseAllocationIdentifier`. Copy the body shape of the neighbouring `newPropertyId()` method exactly.

- [ ] **Step 6: Verify buurman-common compiles**

Run: `cd backend && mvn compile -pl buurman-common -Pquick`

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/domain/ \
        backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java \
        backend/buurman-common/src/main/java/com/buurman/util/SidGenerator.java
git commit -m "feat: add Unit and ExpenseAllocation domain types (BUUR-106)"
```

---

## Task 4: Strip dwelling fields from Property — restore compilation

Fixes the break Task 2 created. Work from the `cannot find symbol` list recorded in Task 2 Step 7.

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/domain/Property.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/mapper/PropertyRecordMapper.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/repository/PropertyRepository.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/repository/TestDataHelper.java`

**Interfaces:**
- Consumes: `UnitStatus` from Task 3.
- Produces: a `Property` domain type with no dwelling fields and no nested `PropertyStatus` enum; `properties.allocation_basis` readable as `Property.getAllocationBasis()` returning `AllocationBasis`.

- [ ] **Step 1: Remove the dwelling fields from Property**

Delete these fields from `Property.java`: `status`, `areaValue`, `areaUnit`, `energyEfficiencyRating`, `energyCertificateExpiryDate`, `heatingType`, `coolingType`, `hotWaterSystem`, `insulationNotes`, `flooringType`, `windowType`, `hasSmokeDetectors`, `hasCoDetectors`, `hasFireExtinguisher`, `hasAdaptedBathroom`, `accessibilityNotes`.

Delete the nested `PropertyStatus` enum entirely — `UnitStatus` replaces it.

Add the allocation basis field:

```java
  @Builder.Default private AllocationBasis allocationBasis = AllocationBasis.EQUAL;
```

- [ ] **Step 2: Update PropertyRecordMapper and PropertyRepository**

Remove every mapping and every `.set(PROPERTIES.<dwelling column>, ...)` line for the deleted fields, in both `toDomain` and `save`. Add `allocationBasis`:

```java
          .set(PROPERTIES.ALLOCATION_BASIS, property.getAllocationBasis().name())
```

and on the read side map `record.getAllocationBasis()` through `AllocationBasis.valueOf(...)`.

**Note:** `PropertyRepository` also has UPDATE branches and any `status`-based filter conditions (e.g. a `findByStatus` or a dashboard vacancy query). Those need rewriting to join `units`. If a method's whole purpose was filtering properties by status, leave a compiling stub that throws `UnsupportedOperationException("Replaced by unit-level status filtering in Task 12")` and note it — Task 12 owns it. Do not silently return an empty list, which would look like working code.

- [ ] **Step 3: Update the integration-test truncation order**

In `AbstractRepositoryIntegrationTest.cleanDatabase()`, replace the `property_amenities` line and add the new tables. They must be deleted **before** `properties` and in reverse-FK order:

```java
    dsl.deleteFrom(DSL.table("expense_allocations")).execute();
    dsl.deleteFrom(DSL.table("unit_amenities")).execute();
    dsl.deleteFrom(DSL.table("unit_residential_details")).execute();
```

and place, immediately before the existing `properties` delete:

```java
    dsl.deleteFrom(DSL.table("units")).execute();
```

Delete the `dsl.deleteFrom(DSL.table("property_amenities")).execute();` line — that table no longer exists. `expenses` must be deleted before `expense_allocations`' parent rows disappear; verify the existing `expenses` delete already precedes `properties` and move it if not.

In `TestDataHelper`, any property-insert helper setting `status`, `area_value` or other dropped columns must stop doing so. Add a `insertUnit(DSLContext, UUID unitId, UUID propertyId, UUID teamId, String unitNumber, UnitStatus status)` helper, since later tasks need it:

```java
  static void insertUnit(
      DSLContext dsl,
      UUID unitId,
      UUID propertyId,
      UUID teamId,
      String unitNumber,
      String status) {
    dsl.insertInto(DSL.table("units"))
        .set(DSL.field("id", UUID.class), unitId)
        .set(DSL.field("identifier", String.class), SidGenerator.newUnitId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("unit_number", String.class), unitNumber)
        .set(DSL.field("unit_type", String.class), "APARTMENT")
        .set(DSL.field("status", String.class), status)
        .set(DSL.field("is_implicit", Boolean.class), false)
        .set(DSL.field("area_unit", String.class), "sqm")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .execute();
  }
```

- [ ] **Step 4: Compile and fix the remaining fallout**

Run: `cd backend && mvn compile -pl buurman-core -am -Pquick`

Iterate until BUILD SUCCESS. Expect fallout in services and mappers that read `property.getStatus()` — `PropertyService`, `OccupancyPeriodService`, dashboard services, WWS services, booklet/export assemblers in `buurman-booklets`, and demo generators. For each, the correct move at this stage is the smallest change that compiles and stays honest:
- a read of `property.getStatus()` used purely for display → leave the call site broken-out into a `TODO(BUUR-106 Task 12)` and derive from units in Task 12;
- anything that would need a unit join → stub with `UnsupportedOperationException` and a message naming the owning task.

Do **not** substitute a hardcoded `UnitStatus.VACANT` to make it compile. That is a silent behaviour change.

- [ ] **Step 5: Run the full backend test suite to see the real damage**

Run: `cd backend && mvn test`

Expected: failures. Record the failing test list; it is the acceptance checklist for Tasks 12–16. `UnitBackfillMigrationIntegrationTest` must still pass.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/domain/Property.java \
        backend/buurman-core/src/main/java/com/buurman/mapper/PropertyRecordMapper.java \
        backend/buurman-core/src/main/java/com/buurman/repository/PropertyRepository.java \
        backend/buurman-core/src/test/java/com/buurman/repository/
git commit -m "refactor: strip dwelling fields from Property, add allocationBasis (BUUR-106)

Property is now building-only. PropertyStatus is replaced by UnitStatus.
Call sites needing a unit join are stubbed and tagged with their owning task."
```

---

## Task 5: UnitRecordMapper, UnitRepository and team-isolation tests

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/UnitRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/UnitRepository.java`
- Create: `backend/buurman-core/src/test/java/com/buurman/repository/UnitRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `Unit`, `UnitType`, `UnitStatus`, `UnitIdentifier` (Task 3); `TestDataHelper.insertUnit` (Task 4).
- Produces:
  - `UnitRepository.findByIdentifierAndTeamId(Sid, UUID) : Optional<Unit>`
  - `UnitRepository.getByIdentifierAndTeamId(Sid, UUID) : Unit`
  - `UnitRepository.findAllByPropertyIdAndTeamId(UUID, UUID) : List<Unit>`
  - `UnitRepository.countActiveByPropertyIdAndTeamId(UUID, UUID) : int`
  - `UnitRepository.countActiveByTeamId(UUID) : int`
  - `UnitRepository.save(Unit) : Unit`
  - `UnitRepository.softDelete(UUID unitId, UUID teamId, UUID actorId) : void`

- [ ] **Step 1: Write the failing repository integration test**

Create `UnitRepositoryIntegrationTest.java`. The third test is Review Focus item 2 — units of a soft-deleted property must not be counted.

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.util.SidGenerator;

@DisplayName("UnitRepository")
class UnitRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private UnitRepository repository;
  private UUID teamAPropertyId;
  private UUID teamBPropertyId;

  @BeforeEach
  void setUpRepository() {
    repository = new UnitRepository(dsl, TestDataHelper.wireMapper(new UnitRecordMapperImpl()), CLOCK);
    teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID, "A Street 1");
    teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID, "B Street 2");
  }

  @Test
  @DisplayName("round-trips a saved unit")
  void savesAndReadsBack() {
    Unit saved =
        repository.save(
            Unit.builder()
                .identifier(java.util.Optional.of(SidGenerator.newUnitId()))
                .teamId(TEAM_A_ID)
                .propertyId(teamAPropertyId)
                .unitNumber("2")
                .unitType(UnitType.APARTMENT)
                .status(UnitStatus.VACANT)
                .createdBy(java.util.Optional.of(USER_ID))
                .updatedBy(java.util.Optional.of(USER_ID))
                .build());

    Unit found =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

    assertThat(found.getUnitNumber()).isEqualTo("2");
    assertThat(found.getUnitType()).isEqualTo(UnitType.APARTMENT);
    assertThat(found.getStatus()).isEqualTo(UnitStatus.VACANT);
    assertThat(found.getPropertyId()).isEqualTo(teamAPropertyId);
  }

  @Test
  @DisplayName("hides one team's units from another team's scoped lookup")
  void isolatesByTeam() {
    UUID teamBUnitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, teamBUnitId, teamBPropertyId, TEAM_B_ID, "1", "VACANT");
    String teamBIdentifier =
        dsl.select(DSL.field("identifier", String.class))
            .from(DSL.table("units"))
            .where(DSL.field("id").eq(teamBUnitId))
            .fetchOne(0, String.class);

    assertThat(repository.findByIdentifierAndTeamId(com.buurman.domain.Sid.of(teamBIdentifier), TEAM_A_ID))
        .isEmpty();
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
```

`TestDataHelper.insertProperty(dsl, teamId, createdBy, street)` returning the new `UUID` may not exist in that exact shape. If it does not, add it in this task following the `insertUnit` helper's style, setting only the post-V070 property columns (no `status`, no `area_value`).

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitRepositoryIntegrationTest'`

Expected: FAIL to compile — `UnitRepository` and `UnitRecordMapperImpl` do not exist.

- [ ] **Step 3: Write UnitRecordMapper**

Open `backend/buurman-core/src/main/java/com/buurman/mapper/PropertyRecordMapper.java` and follow its annotations and `OptionalMappingConfig` usage exactly. `UnitRecordMapper` maps `UnitsRecord` ↔ `Unit`, converting `unit_type` and `status` strings through `UnitType.valueOf` / `UnitStatus.valueOf` and mapping the `is_implicit` column to the `implicit` field.

- [ ] **Step 4: Write UnitRepository**

Follow `PropertyRepository`'s structure: `@Repository @RequiredArgsConstructor`, constructor-injected `DSLContext dsl`, `UnitRecordMapper mapper`, `Clock clock`. Every query filters `team_id` and `deleted_at IS NULL`, and every listing/count **joins `properties` on `deleted_at IS NULL`** so units of a soft-deleted property drop out:

```java
  public List<Unit> findAllByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return List.copyOf(
        dsl.select(UNITS.fields())
            .from(UNITS)
            .join(PROPERTIES)
            .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
            .where(
                UNITS
                    .PROPERTY_ID
                    .eq(propertyId)
                    .and(UNITS.TEAM_ID.eq(teamId))
                    .and(UNITS.DELETED_AT.isNull())
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .orderBy(UNITS.SORT_ORDER.asc(), UNITS.UNIT_NUMBER.asc())
            .fetchInto(UNITS)
            .map(mapper::toDomain));
  }

  public int countActiveByTeamId(UUID teamId) {
    Integer count =
        dsl.selectCount()
            .from(UNITS)
            .join(PROPERTIES)
            .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
            .where(
                UNITS
                    .TEAM_ID
                    .eq(teamId)
                    .and(UNITS.DELETED_AT.isNull())
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .fetchOne(0, Integer.class);
    return count == null ? 0 : count;
  }
```

`save` mirrors `PropertyRepository.save`: INSERT when `getId() == null`, otherwise UPDATE, setting `created_by`/`updated_by` and `created_at`/`updated_at` from `Clock`. `softDelete` sets `deleted_at` and `updated_by`, scoped by `team_id`.

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitRepositoryIntegrationTest'`

Expected: PASS, all four tests.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/mapper/UnitRecordMapper.java \
        backend/buurman-core/src/main/java/com/buurman/repository/UnitRepository.java \
        backend/buurman-core/src/test/java/com/buurman/repository/
git commit -m "feat: add UnitRepository with team and soft-delete scoping (BUUR-106)"
```

---

## Task 6: Unit DTOs and UnitMapper

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/CreateUnitRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/UpdateUnitRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/BulkCreateUnitsRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/UnitResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/UnitSummaryResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/UnitGridRowResponse.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/UnitMapper.java`

**Interfaces:**
- Consumes: `Unit`, `UnitType`, `UnitStatus` (Task 3).
- Produces:
  - `record CreateUnitRequest(String unitNumber, @Nullable String name, @Nullable Integer floor, UnitType unitType, @Nullable UnitStatus status, @Nullable BigDecimal areaValue, @Nullable String areaUnit, @Nullable BigDecimal allocationShare, @Nullable BigDecimal wozSharePct, @Nullable String energyEfficiencyRating, @Nullable String heatingType)`
  - `record BulkCreateUnitsRequest(int count, NumberingPattern numberingPattern, UnitType unitType, @Nullable Integer startFloor)` with `enum NumberingPattern { NUMERIC, ALPHABETIC, FLOOR_DOT_INDEX }`
  - `record UnitResponse(String identifier, String propertyIdentifier, String unitNumber, @Nullable String name, @Nullable Integer floor, UnitType unitType, UnitStatus status, boolean implicit, @Nullable BigDecimal areaValue, ...)`
  - `record UnitSummaryResponse(String identifier, String unitNumber, @Nullable String name, UnitType unitType, UnitStatus status)`
  - `record UnitGridRowResponse(String identifier, String unitNumber, @Nullable String name, UnitType unitType, UnitStatus status, @Nullable String tenantName, @Nullable MoneyAmount monthlyRent, @Nullable Integer vacancyDays)`
  - `UnitMapper.toResponse(Unit) : UnitResponse`, `UnitMapper.toSummary(Unit) : UnitSummaryResponse`

- [ ] **Step 1: Write the request records**

All response DTOs are Java `record` types exposing `identifier` only, never internal UUIDs. Open `backend/buurman-common/src/main/java/com/buurman/dto/request/CreatePropertyRequest.java` for the project's validation-annotation conventions (`@NotNull`, `@Size`, `@Positive`) and mirror them. `CreateUnitRequest.unitNumber` is `@NotBlank @Size(max = 50)`; `unitType` is `@NotNull`; `allocationShare` and `wozSharePct` are `@DecimalMin("0") @DecimalMax("100")`.

`BulkCreateUnitsRequest.count` is `@Min(1) @Max(200)` — 200 is a deliberate guard, well above the "1 to 50 units" positioning, so a typo cannot create ten thousand rows.

- [ ] **Step 2: Write the response records and UnitMapper**

`UnitMapper` is a MapStruct interface with `componentModel = "spring"`, following `PropertyMapper`. It resolves `propertyIdentifier` from a passed-in `Sid` rather than loading the property itself — keep the mapper free of repository calls.

- [ ] **Step 3: Compile**

Run: `cd backend && mvn compile -pl buurman-core -am -Pquick`

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/dto/ \
        backend/buurman-core/src/main/java/com/buurman/mapper/UnitMapper.java
git commit -m "feat: add Unit request and response DTOs (BUUR-106)"
```

---

## Task 7: UnitService — create, update, delete and the never-zero-units invariant

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/UnitService.java`
- Create: `backend/buurman-core/src/test/java/com/buurman/service/UnitServiceTest.java`

**Interfaces:**
- Consumes: `UnitRepository` (Task 5), `PropertyRepository`, `UnitMapper` and the DTOs (Task 6), `UserPrincipal`.
- Produces:
  - `UnitService.createUnit(PropertyIdentifier, CreateUnitRequest, UserPrincipal) : UnitResponse`
  - `UnitService.updateUnit(UnitIdentifier, UpdateUnitRequest, UserPrincipal) : UnitResponse`
  - `UnitService.deleteUnit(UnitIdentifier, UserPrincipal) : void`
  - `UnitService.getUnit(UnitIdentifier, UserPrincipal) : UnitResponse`
  - `UnitService.listUnits(PropertyIdentifier, UserPrincipal) : List<UnitGridRowResponse>`

- [ ] **Step 1: Write the failing service test**

Create `UnitServiceTest.java`. Follow `OccupancyPeriodServiceTest`'s shape: `@ExtendWith(MockitoExtension.class)`, `@DisplayName`, `@Mock` repositories, a fixed `Clock`, and `@Nested` classes per behaviour. The duplicate-number test is Review Focus item 3.

```java
package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitService")
class UnitServiceTest {

  @Mock private UnitRepository unitRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;

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
      when(unitRepository.save(any()))
          .thenThrow(new DuplicateKeyException("uq_units_property_number"));

      CreateUnitRequest request =
          new CreateUnitRequest(
              "1", null, null, UnitType.APARTMENT, null, null, null, null, null, null, null);

      assertThatThrownBy(
              () -> service().createUnit(propertyIdentifier(), request, principal()))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("1");
    }

    @Test
    @DisplayName("promotes the implicit unit instead of adding a second one")
    void promotesImplicitUnit() {
      Unit implicitUnit = unit("1", UnitStatus.VACANT);
      implicitUnit.setImplicit(true);
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(implicitUnit));
      when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      CreateUnitRequest request =
          new CreateUnitRequest(
              "2", null, null, UnitType.APARTMENT, null, null, null, null, null, null, null);

      service().createUnit(propertyIdentifier(), request, principal());

      // Two saves: the promoted implicit unit, then the new one.
      verify(unitRepository, org.mockito.Mockito.times(2)).save(any());
      assertThat(implicitUnit.isImplicit()).isFalse();
    }
  }

  // --- fixtures: fill these in following OccupancyPeriodServiceTest's helper style ---

  private UnitService service() {
    return new UnitService(unitRepository, propertyRepository, contractRepository, null, clock);
  }

  private Unit unit(String number, UnitStatus status) {
    return Unit.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(com.buurman.util.SidGenerator.newUnitId()))
        .teamId(TEAM_ID)
        .propertyId(PROPERTY_ID)
        .unitNumber(number)
        .unitType(UnitType.APARTMENT)
        .status(status)
        .build();
  }

  private Property property() {
    return Property.builder().id(PROPERTY_ID).teamId(TEAM_ID).build();
  }

  private UnitIdentifier unitIdentifier() {
    return UnitIdentifier.of(com.buurman.util.SidGenerator.newUnitId().value());
  }

  private com.buurman.domain.identifier.PropertyIdentifier propertyIdentifier() {
    return com.buurman.domain.identifier.PropertyIdentifier.of(
        com.buurman.util.SidGenerator.newPropertyId().value());
  }

  private UserPrincipal principal() {
    // Match how OccupancyPeriodServiceTest builds its principal.
    return UserPrincipal.builder().userId(USER_ID).teamId(TEAM_ID).build();
  }

  private static UUID eqTeam() {
    return org.mockito.ArgumentMatchers.eq(TEAM_ID);
  }
}
```

**Note for the implementer:** `UserPrincipal` and `UnitIdentifier` constructor shapes must match the real classes — read `OccupancyPeriodServiceTest` and `PropertyIdentifier` and adjust the three fixture helpers accordingly. `ContractRepository.countActiveByUnitId(UUID, UUID)` does not exist yet; add it in this task alongside the service, filtering `team_id`, `unit_id`, `status = 'ACTIVE'` and `deleted_at IS NULL`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitServiceTest'`

Expected: FAIL to compile — `UnitService` does not exist.

- [ ] **Step 3: Write UnitService**

`@Service @RequiredArgsConstructor @Transactional`. Authorization matches `PropertyService`: writes carry `@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")`, `deleteUnit` carries `@PreAuthorize("hasRole('TEAM_ADMIN')")`, reads stay unannotated and team-scoped.

`deleteUnit` enforces both invariants before touching the repository:

```java
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteUnit(UnitIdentifier identifier, UserPrincipal principal) {
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    if (unitRepository.countActiveByPropertyIdAndTeamId(unit.getPropertyId(), principal.getTeamId())
        <= 1) {
      throw new BusinessRuleException(
          "Cannot delete the last unit of a property. A property must always have at least one unit.");
    }
    if (contractRepository.countActiveByUnitId(unit.getId(), principal.getTeamId()) > 0) {
      throw new BusinessRuleException(
          "Cannot delete a unit with an active contract. End the contract first.");
    }
    unitRepository.softDelete(unit.getId(), principal.getTeamId(), principal.getUserId());
  }
```

`createUnit` promotes the implicit unit before inserting, and translates the unique-index violation:

```java
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public UnitResponse createUnit(
      PropertyIdentifier propertyIdentifier, CreateUnitRequest request, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.getTeamId());

    promoteImplicitUnit(property.getId(), principal);

    Unit unit =
        Unit.builder()
            .identifier(Optional.of(SidGenerator.newUnitId()))
            .teamId(principal.getTeamId())
            .propertyId(property.getId())
            .unitNumber(request.unitNumber())
            .name(Optional.ofNullable(request.name()))
            .floor(Optional.ofNullable(request.floor()))
            .unitType(request.unitType())
            .status(Optional.ofNullable(request.status()).orElse(UnitStatus.VACANT))
            .areaValue(Optional.ofNullable(request.areaValue()))
            .areaUnit(Optional.ofNullable(request.areaUnit()))
            .allocationShare(Optional.ofNullable(request.allocationShare()))
            .wozSharePct(Optional.ofNullable(request.wozSharePct()))
            .energyEfficiencyRating(Optional.ofNullable(request.energyEfficiencyRating()))
            .heatingType(Optional.ofNullable(request.heatingType()))
            .createdBy(Optional.of(principal.getUserId()))
            .updatedBy(Optional.of(principal.getUserId()))
            .build();

    try {
      return unitMapper.toResponse(unitRepository.save(unit));
    } catch (DuplicateKeyException e) {
      throw new BusinessRuleException(
          "A unit numbered " + request.unitNumber() + " already exists on this property.");
    }
  }

  /**
   * Flips the property's implicit stand-in unit into a real one so it keeps its contracts, photos,
   * occupancy history and WWS calculations when the landlord starts tracking units explicitly.
   */
  private void promoteImplicitUnit(UUID propertyId, UserPrincipal principal) {
    unitRepository.findAllByPropertyIdAndTeamId(propertyId, principal.getTeamId()).stream()
        .filter(Unit::isImplicit)
        .findFirst()
        .ifPresent(
            implicitUnit -> {
              implicitUnit.setImplicit(false);
              implicitUnit.setUpdatedBy(Optional.of(principal.getUserId()));
              unitRepository.save(implicitUnit);
            });
  }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitServiceTest'`

Expected: PASS, all five tests.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/UnitService.java \
        backend/buurman-core/src/main/java/com/buurman/repository/ContractRepository.java \
        backend/buurman-core/src/test/java/com/buurman/service/UnitServiceTest.java
git commit -m "feat: add UnitService with never-zero-units and implicit-promotion rules (BUUR-106)"
```

---

## Task 8: Bulk unit creation

**Files:**
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/UnitService.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/service/UnitServiceTest.java`

**Interfaces:**
- Consumes: everything from Task 7.
- Produces: `UnitService.bulkCreateUnits(PropertyIdentifier, BulkCreateUnitsRequest, UserPrincipal) : List<UnitResponse>`

- [ ] **Step 1: Write the failing bulk-create tests**

Add a `@Nested` class to `UnitServiceTest`:

```java
  @Nested
  @DisplayName("bulkCreateUnits")
  class BulkCreateUnits {

    @Test
    @DisplayName("reuses the implicit unit as #1 and inserts the rest")
    void reusesImplicitUnitAsFirst() {
      Unit implicitUnit = unit("1", UnitStatus.OCCUPIED);
      implicitUnit.setImplicit(true);
      UUID implicitId = implicitUnit.getId();
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(implicitUnit));
      when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      List<UnitResponse> created =
          service()
              .bulkCreateUnits(
                  propertyIdentifier(),
                  new BulkCreateUnitsRequest(
                      6, BulkCreateUnitsRequest.NumberingPattern.NUMERIC, UnitType.APARTMENT, null),
                  principal());

      assertThat(created).hasSize(6);
      // The promoted unit keeps its identity — it must not be replaced.
      assertThat(implicitUnit.getId()).isEqualTo(implicitId);
      assertThat(implicitUnit.isImplicit()).isFalse();
      assertThat(implicitUnit.getStatus()).isEqualTo(UnitStatus.OCCUPIED);
    }

    @Test
    @DisplayName("numbers units 1..n for NUMERIC")
    void numbersNumerically() {
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      List<UnitResponse> created =
          service()
              .bulkCreateUnits(
                  propertyIdentifier(),
                  new BulkCreateUnitsRequest(
                      3, BulkCreateUnitsRequest.NumberingPattern.NUMERIC, UnitType.APARTMENT, null),
                  principal());

      assertThat(created).extracting(UnitResponse::unitNumber).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("numbers units A..N for ALPHABETIC")
    void numbersAlphabetically() {
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      List<UnitResponse> created =
          service()
              .bulkCreateUnits(
                  propertyIdentifier(),
                  new BulkCreateUnitsRequest(
                      3,
                      BulkCreateUnitsRequest.NumberingPattern.ALPHABETIC,
                      UnitType.APARTMENT,
                      null),
                  principal());

      assertThat(created).extracting(UnitResponse::unitNumber).containsExactly("A", "B", "C");
    }

    @Test
    @DisplayName("numbers units <floor>.<index> for FLOOR_DOT_INDEX")
    void numbersByFloor() {
      when(propertyRepository.getByIdentifierAndTeamId(any(), eqTeam())).thenReturn(property());
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      List<UnitResponse> created =
          service()
              .bulkCreateUnits(
                  propertyIdentifier(),
                  new BulkCreateUnitsRequest(
                      3,
                      BulkCreateUnitsRequest.NumberingPattern.FLOOR_DOT_INDEX,
                      UnitType.APARTMENT,
                      1),
                  principal());

      assertThat(created)
          .extracting(UnitResponse::unitNumber)
          .containsExactly("1.01", "1.02", "1.03");
    }
  }
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitServiceTest$BulkCreateUnits'`

Expected: FAIL to compile — `bulkCreateUnits` does not exist.

- [ ] **Step 3: Implement bulkCreateUnits**

The implicit unit is promoted and renumbered to the batch's first label rather than being deleted and re-created, so its contracts and history survive:

```java
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<UnitResponse> bulkCreateUnits(
      PropertyIdentifier propertyIdentifier,
      BulkCreateUnitsRequest request,
      UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(
            propertyIdentifier, principal.getTeamId());

    List<String> labels = numberingLabels(request);
    List<Unit> existing =
        unitRepository.findAllByPropertyIdAndTeamId(property.getId(), principal.getTeamId());
    Optional<Unit> implicitUnit = existing.stream().filter(Unit::isImplicit).findFirst();

    List<UnitResponse> created = new ArrayList<>();

    for (int i = 0; i < labels.size(); i++) {
      String label = labels.get(i);
      if (i == 0 && implicitUnit.isPresent()) {
        Unit promoted = implicitUnit.get();
        promoted.setImplicit(false);
        promoted.setUnitNumber(label);
        promoted.setSortOrder(i);
        promoted.setUpdatedBy(Optional.of(principal.getUserId()));
        created.add(unitMapper.toResponse(unitRepository.save(promoted)));
      } else {
        created.add(
            unitMapper.toResponse(
                unitRepository.save(newUnit(property.getId(), label, i, request, principal))));
      }
    }
    return List.copyOf(created);
  }

  private List<String> numberingLabels(BulkCreateUnitsRequest request) {
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < request.count(); i++) {
      switch (request.numberingPattern()) {
        case NUMERIC -> labels.add(String.valueOf(i + 1));
        case ALPHABETIC -> labels.add(alphabeticLabel(i));
        case FLOOR_DOT_INDEX ->
            labels.add(
                Optional.ofNullable(request.startFloor()).orElse(0)
                    + "."
                    + String.format("%02d", i + 1));
      }
    }
    return labels;
  }

  /** 0 -> "A", 25 -> "Z", 26 -> "AA". Spreadsheet-column style, so counts above 26 stay unique. */
  private String alphabeticLabel(int index) {
    StringBuilder label = new StringBuilder();
    int remaining = index;
    while (remaining >= 0) {
      label.insert(0, (char) ('A' + (remaining % 26)));
      remaining = (remaining / 26) - 1;
    }
    return label.toString();
  }
```

- [ ] **Step 4: Run to verify it passes**

Run: `cd backend && mvn test -pl buurman-core -Dtest='UnitServiceTest'`

Expected: PASS, all nine tests.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/UnitService.java \
        backend/buurman-core/src/test/java/com/buurman/service/UnitServiceTest.java
git commit -m "feat: bulk-create units, promoting the implicit unit in place (BUUR-106)"
```

---

## Task 9: UnitController and the OpenAPI contract

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/controller/UnitController.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/UnitResidentialDetailsRepository.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/UnitAmenityRepository.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/UnitResidentialDetailsResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/UpdateUnitResidentialDetailsRequest.java`
- Create: `openapi/src/paths/units.yaml`
- Modify: `openapi/src/app.yaml`
- Delete: the property-level residential-details and amenities controller/service/repository code that V070 orphaned

**Interfaces:**
- Consumes: `UnitService` (Tasks 7–8).
- Produces: the endpoints listed below, plus generated frontend client hooks consumed by plan 2.

- [ ] **Step 1: Write the controller**

Thin, delegating straight to the service — follow `PropertyController`. Endpoints:

```
GET     /properties/{propertyIdentifier}/units       -> List<UnitGridRowResponse>
POST    /properties/{propertyIdentifier}/units       -> UnitResponse        201
POST    /properties/{propertyIdentifier}/units/bulk  -> List<UnitResponse>  201
GET     /units/{identifier}                          -> UnitResponse
PUT     /units/{identifier}                          -> UnitResponse
DELETE  /units/{identifier}                          -> 204
GET     /units/{identifier}/residential-details      -> UnitResidentialDetailsResponse
PUT     /units/{identifier}/residential-details      -> UnitResidentialDetailsResponse
GET     /units/{identifier}/amenities                -> List<AmenityResponse>
PUT     /units/{identifier}/amenities                -> List<AmenityResponse>
```

The residential-details and amenities pairs replace the property-level equivalents that V070 dropped. Find the existing property-level handlers — search `grep -rn "residential-details\|amenities" backend/buurman-core/src/main/java/com/buurman/controller/` — and move them onto the unit, reusing `AmenityResponse` unchanged. `UnitResidentialDetailsResponse` is a new record `(Integer bedrooms, Integer bathrooms, boolean furnished, String petPolicy)` mirroring the dropped `PropertyResidentialDetailsResponse`. A `UnitResidentialDetailsRepository` and `UnitAmenityRepository` follow the `UnitRepository` pattern from Task 5, both filtering `team_id`.

Read `PropertyController` for the project's `@Tag`, `@Operation`, `@ApiResponse` and `@Valid` conventions and mirror them. The controller carries no authorization annotations — those live on the service.

- [ ] **Step 2: Write units.yaml and register it**

Create `openapi/src/paths/units.yaml` following `properties.yaml`'s structure, using `$ref: '#/components/schemas/...'` for every schema. Add the schemas `Unit`, `UnitSummary`, `UnitGridRow`, `CreateUnitRequest`, `UpdateUnitRequest`, `BulkCreateUnitsRequest`, `UnitType`, `UnitStatus`, `AllocationBasis` to `openapi/src/app.yaml`'s `components.schemas`, and add the `$ref` entries for the new paths.

- [ ] **Step 3: Bundle and verify**

Run: `make bundle-openapi`

Expected: `openapi/app.yaml` regenerates. Confirm the new paths appear:

Run: `grep -c "units" openapi/app.yaml`

Expected: a non-zero count.

- [ ] **Step 4: Verify the app boots and the endpoints are live**

Run: `cd backend && mvn spring-boot:run -pl buurman-app -am` (needs `make dev` infrastructure running), then in another shell:

Run: `curl -s -o /dev/null -w '%{http_code}' https://api.local.buurman.io/units/UNTdoesnotexist`

Expected: `401` (unauthenticated) rather than `404` from the router — proving the route is mapped. Stop the app afterwards.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/controller/UnitController.java \
        openapi/src/paths/units.yaml openapi/src/app.yaml openapi/app.yaml
git commit -m "feat: add UnitController and OpenAPI unit contract (BUUR-106)"
```

---

## Task 10: The allocation engine

Owns three Review Focus items: zero-area `AREA` fallback (1), negative amounts (4), and currency inheritance (5).

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/ExpenseAllocationService.java`
- Create: `backend/buurman-core/src/test/java/com/buurman/service/ExpenseAllocationServiceTest.java`

**Interfaces:**
- Consumes: `Unit`, `AllocationBasis`, `ExpenseAllocation`, `MoneyAmount`, `UnitRepository`.
- Produces:
  - `ExpenseAllocationService.computeAllocations(MoneyAmount total, AllocationBasis basis, List<Unit> units) : List<ExpenseAllocation>` — pure, no I/O, so it is directly unit-testable
  - `ExpenseAllocationService.allocate(Expense, UserPrincipal) : List<ExpenseAllocationResponse>` — persists
  - `ExpenseAllocationService.recompute(ExpenseIdentifier, UserPrincipal) : List<ExpenseAllocationResponse>`

- [ ] **Step 1: Write the failing allocation tests**

```java
package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseAllocationService.computeAllocations")
class ExpenseAllocationServiceTest {

  private final ExpenseAllocationService service = new ExpenseAllocationService(null, null, null);

  @Test
  @DisplayName("splits a 1200.00 expense equally across 4 units as 300.00 each")
  void splitsEquallyWithoutRemainder() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("1200.00"), AllocationBasis.EQUAL, units(null, null, null, null));

    assertThat(allocations).hasSize(4);
    assertThat(allocations)
        .allSatisfy(a -> assertThat(a.getAmount().value()).isEqualByComparingTo("300.00"));
    assertThat(sum(allocations)).isEqualByComparingTo("1200.00");
  }

  @Test
  @DisplayName("distributes an indivisible remainder by largest remainder, summing exactly")
  void distributesRemainderExactly() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("100.00"), AllocationBasis.EQUAL, units(null, null, null));

    assertThat(sum(allocations)).isEqualByComparingTo("100.00");
    assertThat(allocations)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactlyInAnyOrder("33.34", "33.33", "33.33");
  }

  @Test
  @DisplayName("splits by area under AREA basis")
  void splitsByArea() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("900.00"),
            AllocationBasis.AREA,
            units(new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50")));

    assertThat(allocations)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactly("450.00", "225.00", "225.00");
    assertThat(sum(allocations)).isEqualByComparingTo("900.00");
  }

  @Test
  @DisplayName("falls back to EQUAL and records it when no unit has an area")
  void fallsBackToEqualWhenNoAreas() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("300.00"), AllocationBasis.AREA, units(null, null, null));

    assertThat(sum(allocations)).isEqualByComparingTo("300.00");
    assertThat(allocations).allSatisfy(a -> assertThat(a.getBasis()).isEqualTo(AllocationBasis.EQUAL));
  }

  @Test
  @DisplayName("treats a unit with no area as a zero share when siblings have areas")
  void treatsMissingAreaAsZeroShare() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("300.00"), AllocationBasis.AREA, units(new BigDecimal("100"), null));

    assertThat(allocations.get(0).getAmount().value()).isEqualByComparingTo("300.00");
    assertThat(allocations.get(1).getAmount().value()).isEqualByComparingTo("0.00");
    assertThat(sum(allocations)).isEqualByComparingTo("300.00");
  }

  @Test
  @DisplayName("sums exactly for a negative amount, such as a credit note")
  void handlesNegativeAmounts() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("-100.00"), AllocationBasis.EQUAL, units(null, null, null));

    assertThat(sum(allocations)).isEqualByComparingTo("-100.00");
    assertThat(allocations)
        .allSatisfy(a -> assertThat(a.getAmount().value()).isNegative());
  }

  @Test
  @DisplayName("gives every allocation row the expense's own currency")
  void inheritsExpenseCurrency() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            new MoneyAmount(new BigDecimal("500.00"), "SEK"),
            AllocationBasis.EQUAL,
            units(null, null));

    assertThat(allocations).allSatisfy(a -> assertThat(a.getAmount().currency()).isEqualTo("SEK"));
  }

  @Test
  @DisplayName("splits by allocation_share under CUSTOM basis")
  void splitsByCustomShare() {
    List<Unit> unitList = units(null, null);
    unitList.get(0).setAllocationShare(Optional.of(new BigDecimal("70")));
    unitList.get(1).setAllocationShare(Optional.of(new BigDecimal("30")));

    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("1000.00"), AllocationBasis.CUSTOM, unitList);

    assertThat(allocations)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactly("700.00", "300.00");
  }

  @Test
  @DisplayName("returns no allocations for an empty unit list")
  void handlesNoUnits() {
    assertThat(service.computeAllocations(money("100.00"), AllocationBasis.EQUAL, List.of()))
        .isEmpty();
  }

  private static MoneyAmount money(String value) {
    return new MoneyAmount(new BigDecimal(value), "EUR");
  }

  private static BigDecimal sum(List<ExpenseAllocation> allocations) {
    return allocations.stream()
        .map(a -> a.getAmount().value())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static List<Unit> units(BigDecimal... areas) {
    java.util.List<Unit> result = new java.util.ArrayList<>();
    for (int i = 0; i < areas.length; i++) {
      result.add(
          Unit.builder()
              .id(UUID.randomUUID())
              .teamId(UUID.randomUUID())
              .propertyId(UUID.randomUUID())
              .unitNumber(String.valueOf(i + 1))
              .unitType(UnitType.APARTMENT)
              .status(UnitStatus.VACANT)
              .areaValue(Optional.ofNullable(areas[i]))
              .sortOrder(i)
              .build());
    }
    return result;
  }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest='ExpenseAllocationServiceTest'`

Expected: FAIL to compile — `ExpenseAllocationService` does not exist.

- [ ] **Step 3: Implement computeAllocations**

Work entirely in minor units so rounding is exact, then convert back. Largest-remainder: floor every share, then hand the leftover minor units out one at a time to the largest fractional remainders. `abs`-based comparison keeps negative totals correct.

```java
  /**
   * Splits {@code total} across {@code units}. Pure and I/O-free so it can be unit-tested directly.
   *
   * <p>Arithmetic happens in minor units and the remainder is distributed by the largest-remainder
   * method, so the returned amounts always sum to exactly {@code total} — no drifting cent. Works
   * for negative totals (credit notes) because the leftover is signed.
   */
  public List<ExpenseAllocation> computeAllocations(
      MoneyAmount total, AllocationBasis basis, List<Unit> units) {
    if (units.isEmpty()) {
      return List.of();
    }

    AllocationBasis effectiveBasis = resolveBasis(basis, units);
    List<BigDecimal> weights = weightsFor(effectiveBasis, units);
    BigDecimal weightTotal = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    long totalMinor = total.toMinorUnits();
    long[] amounts = new long[units.size()];
    BigDecimal[] remainders = new BigDecimal[units.size()];
    long distributed = 0;

    for (int i = 0; i < units.size(); i++) {
      BigDecimal exact =
          weightTotal.signum() == 0
              ? BigDecimal.ZERO
              : BigDecimal.valueOf(totalMinor)
                  .multiply(weights.get(i))
                  .divide(weightTotal, 10, RoundingMode.HALF_UP);
      amounts[i] = exact.setScale(0, RoundingMode.DOWN).longValueExact();
      remainders[i] = exact.subtract(BigDecimal.valueOf(amounts[i])).abs();
      distributed += amounts[i];
    }

    long leftover = totalMinor - distributed;
    long step = leftover >= 0 ? 1 : -1;
    List<Integer> order =
        IntStream.range(0, units.size())
            .boxed()
            .sorted((a, b) -> remainders[b].compareTo(remainders[a]))
            .toList();

    int cursor = 0;
    while (leftover != 0 && !order.isEmpty()) {
      amounts[order.get(cursor % order.size())] += step;
      leftover -= step;
      cursor++;
    }

    List<ExpenseAllocation> allocations = new ArrayList<>();
    for (int i = 0; i < units.size(); i++) {
      allocations.add(
          ExpenseAllocation.builder()
              .unitId(units.get(i).getId())
              .teamId(units.get(i).getTeamId())
              .amount(minorToMoney(amounts[i], total.currency()))
              .basis(effectiveBasis)
              .build());
    }
    return List.copyOf(allocations);
  }

  /** AREA is only meaningful when at least one unit has an area; otherwise EQUAL is honest. */
  private AllocationBasis resolveBasis(AllocationBasis requested, List<Unit> units) {
    if (requested == AllocationBasis.AREA
        && units.stream().allMatch(u -> u.getAreaValue().isEmpty())) {
      return AllocationBasis.EQUAL;
    }
    if (requested == AllocationBasis.CUSTOM
        && units.stream().allMatch(u -> u.getAllocationShare().isEmpty())) {
      return AllocationBasis.EQUAL;
    }
    return requested;
  }

  private List<BigDecimal> weightsFor(AllocationBasis basis, List<Unit> units) {
    return units.stream()
        .map(
            unit ->
                switch (basis) {
                  case AREA -> unit.getAreaValue().orElse(BigDecimal.ZERO);
                  case CUSTOM -> unit.getAllocationShare().orElse(BigDecimal.ZERO);
                  case EQUAL, MANUAL -> BigDecimal.ONE;
                })
        .toList();
  }

  private MoneyAmount minorToMoney(long minor, String currency) {
    int digits = CurrencyUtils.getFractionalDigits(currency);
    return new MoneyAmount(BigDecimal.valueOf(minor, digits), currency);
  }
```

- [ ] **Step 4: Run to verify it passes**

Run: `cd backend && mvn test -pl buurman-core -Dtest='ExpenseAllocationServiceTest'`

Expected: PASS, all nine tests. If `distributesRemainderExactly` reports `33.33/33.33/33.34` in a different order, the assertion already uses `containsExactlyInAnyOrder` — but the sum assertion must hold exactly.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/ExpenseAllocationService.java \
        backend/buurman-core/src/test/java/com/buurman/service/ExpenseAllocationServiceTest.java
git commit -m "feat: add expense allocation engine with largest-remainder rounding (BUUR-106)"
```

---

## Task 11: Persist allocations and expose the allocation endpoints

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/ExpenseAllocationRepository.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/ExpenseAllocationRecordMapper.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/ExpenseAllocationResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/ManualAllocationRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/UpdateAllocationRequest.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ExpenseAllocationService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ExpenseService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/controller/ExpenseController.java`
- Modify: `openapi/src/paths/expenses.yaml`, `openapi/src/paths/properties.yaml`, `openapi/src/app.yaml`

**Interfaces:**
- Consumes: `computeAllocations` (Task 10), `UnitRepository` (Task 5).
- Produces:
  - `ExpenseAllocationRepository.replaceForExpense(UUID expenseId, UUID teamId, List<ExpenseAllocation>, UUID actorId) : List<ExpenseAllocation>`
  - `ExpenseAllocationRepository.findByExpenseIdAndTeamId(UUID, UUID) : List<ExpenseAllocation>`
  - `ExpenseAllocationRepository.findByUnitIdAndTeamId(UUID, UUID) : List<ExpenseAllocation>`
  - `record ExpenseAllocationResponse(String identifier, String unitIdentifier, String unitNumber, MoneyAmount amount, AllocationBasis basis)`
  - `record ManualAllocationRequest(List<ManualAllocationEntry> entries)` with `record ManualAllocationEntry(String unitIdentifier, BigDecimal amount)`
  - `record UpdateAllocationRequest(AllocationBasis basis, @Nullable List<UnitShareEntry> shares)` with `record UnitShareEntry(String unitIdentifier, BigDecimal sharePct)`

- [ ] **Step 1: Write the repository and mapper**

Follow `PropertyRepository` and `PropertyRecordMapper`. `replaceForExpense` soft-deletes the existing rows for that expense and inserts the new set in one transaction, so `uq_expense_allocations_expense_unit` never trips on a re-allocation. Every query filters `team_id`.

- [ ] **Step 2: Wire allocation into expense create and update**

In `ExpenseService`, after saving an expense whose `unitId` is empty, call `ExpenseAllocationService.allocate`. When `unitId` is present the expense belongs to one unit and no allocation rows are written. Read the property's `allocationBasis`, load its active units via `UnitRepository.findAllByPropertyIdAndTeamId`, call `computeAllocations`, then `replaceForExpense`.

`MANUAL` basis validates that the supplied entries sum to the expense total and throws `BusinessRuleException` naming the discrepancy if not:

```java
    if (basis == AllocationBasis.MANUAL) {
      BigDecimal supplied =
          request.entries().stream()
              .map(ManualAllocationEntry::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (supplied.compareTo(expense.getAmount().value()) != 0) {
        throw new BusinessRuleException(
            "Manual allocations total "
                + supplied.toPlainString()
                + " but the expense is "
                + expense.getAmount().value().toPlainString()
                + ".");
      }
    }
```

- [ ] **Step 3: Add the endpoints**

```
GET   /expenses/{identifier}/allocations            -> List<ExpenseAllocationResponse>
PUT   /expenses/{identifier}/allocations            -> List<ExpenseAllocationResponse>  (MANUAL override)
POST  /expenses/{identifier}/allocations/recompute  -> List<ExpenseAllocationResponse>
PUT   /properties/{identifier}/allocation           -> PropertyResponse                 (basis + shares)
```

`recompute` exists precisely because changing a property's basis must **not** silently rewrite history; it is the explicit opt-in.

- [ ] **Step 4: Write an integration test for replace-on-recompute**

Create `backend/buurman-core/src/test/java/com/buurman/repository/ExpenseAllocationRepositoryIntegrationTest.java` extending `AbstractRepositoryIntegrationTest`. Assert that calling `replaceForExpense` twice leaves exactly one active row per unit (the second set), that the first set is soft-deleted rather than removed, and that a team-B-scoped read sees none of team A's rows.

- [ ] **Step 5: Run the tests**

Run: `cd backend && mvn test -pl buurman-core -Dtest='ExpenseAllocation*'`

Expected: PASS.

- [ ] **Step 6: Bundle OpenAPI and commit**

```bash
make bundle-openapi
git add backend/buurman-core/src/main/java/com/buurman/ backend/buurman-common/src/main/java/com/buurman/dto/ openapi/
git commit -m "feat: persist expense allocations and expose allocation endpoints (BUUR-106)"
```

---

## Task 12: Property responses, derived status and inline unit on create

Clears the `UnsupportedOperationException` stubs left in Task 4.

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/response/PropertyResponse.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/request/CreatePropertyRequest.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/PropertyService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/repository/PropertyRepository.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/mapper/PropertyMapper.java`
- Modify: `openapi/src/paths/properties.yaml`, `openapi/src/app.yaml`

**Interfaces:**
- Consumes: `UnitService`, `UnitRepository`, `UnitSummaryResponse`.
- Produces:
  - `PropertyResponse` gains `int unitCount`, `int occupiedUnitCount`, `int vacantUnitCount`, `AllocationBasis allocationBasis`, `List<UnitSummaryResponse> units`; loses every dwelling field.
  - `CreatePropertyRequest` gains `@Nullable CreateUnitRequest unit`.
  - `PropertyRepository.findUnitCountsByTeamId(UUID) : Map<UUID, UnitCounts>` where `record UnitCounts(int total, int occupied)` — one grouped query, so a property list of 50 does not fire 50 count queries.

- [ ] **Step 1: Write the failing tests**

Add to `backend/buurman-core/src/test/java/com/buurman/service/PropertyServiceTest.java` (create it if absent, following `OccupancyPeriodServiceTest`):

- `createProperty` with a `unit` payload creates exactly one unit carrying those dwelling values, flagged **not** implicit.
- `createProperty` with `unit == null` creates exactly one unit flagged `implicit`, status `VACANT`, `unitNumber` `"1"`, `allocationShare` `100`.
- `getProperty` on a property with 4 units, 1 occupied, returns `unitCount == 4`, `occupiedUnitCount == 1`, `vacantUnitCount == 3`. This is the ticket's 25%-occupancy acceptance criterion at the service level.

- [ ] **Step 2: Run to verify they fail**

Run: `cd backend && mvn test -pl buurman-core -Dtest='PropertyServiceTest'`

Expected: FAIL.

- [ ] **Step 3: Implement**

`createProperty` always creates a unit, inside the same transaction as the property insert, so the never-zero-units invariant holds from the first moment:

```java
    UUID propertyId = propertyRepository.save(property).getId();
    CreateUnitRequest unitRequest =
        Optional.ofNullable(request.unit())
            .orElseGet(
                () ->
                    new CreateUnitRequest(
                        "1", null, null, defaultUnitType(request.propertyCategory()),
                        UnitStatus.VACANT, null, null, new BigDecimal("100"), null, null, null));
    unitService.createInitialUnit(
        propertyId, unitRequest, request.unit() == null, principal);
```

`createInitialUnit(UUID propertyId, CreateUnitRequest request, boolean implicit, UserPrincipal principal)` is a new package-private method on `UnitService` that skips `promoteImplicitUnit` (there is nothing to promote) and sets the `implicit` flag from its argument.

Replace the Task 4 stubs in `PropertyRepository`: any former status filter becomes an `EXISTS` over `units`:

```java
  private Condition hasUnitWithStatus(UnitStatus status) {
    return DSL.exists(
        DSL.selectOne()
            .from(UNITS)
            .where(
                UNITS
                    .PROPERTY_ID
                    .eq(PROPERTIES.ID)
                    .and(UNITS.STATUS.eq(status.name()))
                    .and(UNITS.DELETED_AT.isNull())));
  }
```

- [ ] **Step 4: Run the tests**

Run: `cd backend && mvn test -pl buurman-core -Dtest='PropertyServiceTest'`

Expected: PASS. Then `grep -rn "UnsupportedOperationException(\"Replaced by unit" backend/` must return nothing.

- [ ] **Step 5: Commit**

```bash
make bundle-openapi
git add backend/ openapi/
git commit -m "feat: derive property status from units, add inline unit on create (BUUR-106)"
```

---

## Task 13: Contract creation resolves the unit

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/request/CreateContractRequest.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ContractService.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/service/ContractServiceTest.java`
- Modify: `openapi/src/paths/contracts.yaml`, `openapi/src/app.yaml`

**Interfaces:**
- Consumes: `UnitRepository`.
- Produces: `CreateContractRequest` gains `@Nullable String unitIdentifier`; `ContractResponse` gains `String unitIdentifier` and `String unitNumber`.

- [ ] **Step 1: Write the failing tests**

Add a `@Nested` class to `ContractServiceTest`:

- omitted `unitIdentifier` + property with exactly one unit → contract gets that unit's id;
- omitted `unitIdentifier` + property with 3 units → `BadRequestException` whose message names the property and says which unit must be chosen;
- supplied `unitIdentifier` belonging to a **different** property → `BadRequestException`, not a silent cross-property contract;
- supplied `unitIdentifier` from **another team** → `NotFoundException` (never leak existence across tenants).

- [ ] **Step 2: Run to verify they fail**

Run: `cd backend && mvn test -pl buurman-core -Dtest='ContractServiceTest'`

Expected: FAIL.

- [ ] **Step 3: Implement the resolution**

```java
  private UUID resolveUnitId(
      Property property, @Nullable String unitIdentifier, UserPrincipal principal) {
    if (unitIdentifier != null) {
      Unit unit =
          unitRepository.getByIdentifierAndTeamId(Sid.of(unitIdentifier), principal.getTeamId());
      if (!unit.getPropertyId().equals(property.getId())) {
        throw new BadRequestException("The chosen unit does not belong to this property.");
      }
      return unit.getId();
    }
    List<Unit> units =
        unitRepository.findAllByPropertyIdAndTeamId(property.getId(), principal.getTeamId());
    if (units.size() != 1) {
      throw new BadRequestException(
          "This property has " + units.size() + " units. Specify which unit the contract is for.");
    }
    return units.get(0).getId();
  }
```

- [ ] **Step 4: Run the tests and commit**

Run: `cd backend && mvn test -pl buurman-core -Dtest='ContractServiceTest'`

Expected: PASS.

```bash
make bundle-openapi
git add backend/ openapi/
git commit -m "feat: resolve the unit when creating a contract (BUUR-106)"
```

---

## Task 14: Occupancy periods and WWS move to the unit

**Files:**
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/OccupancyPeriodService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/repository/PropertyOccupancyPeriodRepository.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/WwsPointsCalculatorService.java` and the WWS repository
- Modify: `backend/buurman-core/src/test/java/com/buurman/service/OccupancyPeriodServiceTest.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/service/WwsPointsCalculatorServiceTest.java`
- Modify: `openapi/src/paths/occupancy.yaml`, `openapi/src/paths/rent-regulations.yaml`, `openapi/src/app.yaml`

**Interfaces:**
- Produces: occupancy and WWS request/response DTOs carry `unitIdentifier`; WWS reads dwelling attributes from `Unit`, not `Property`.

- [ ] **Step 1: Write the failing tests**

- Two self-occupancy periods overlapping on the **same** unit → rejected by the re-scoped exclusion constraint, surfaced as `BusinessRuleException`.
- Two overlapping periods on **different units of the same property** → allowed. This is the whole point of the re-scope and would have been impossible before.
- `WwsPointsCalculatorService` reads `areaValue` and `energyEfficiencyRating` from the unit; two units of one building with different energy labels produce different point totals.

- [ ] **Step 2: Run to verify they fail, then implement**

Run: `cd backend && mvn test -pl buurman-core -Dtest='OccupancyPeriodServiceTest,WwsPointsCalculatorServiceTest'`

Then thread `unitId` through both services. Every occupancy query that grouped by `property_id` groups by `unit_id`; vacancy-day computation becomes per unit, and the property-level figure is the sum across its units.

- [ ] **Step 3: Run and commit**

Expected: PASS.

```bash
make bundle-openapi
git add backend/ openapi/
git commit -m "feat: scope occupancy periods and WWS calculations to units (BUUR-106)"
```

---

## Task 15: billableUnitCount and the backoffice Units column

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/response/TeamResponse.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/TeamService.java`
- Modify: `backend/buurman-backoffice/src/main/java/com/buurman/service/backoffice/` — the team-listing service
- Modify: `openapi/src/paths/teams.yaml`, `openapi/src/app.yaml`

**Interfaces:**
- Consumes: `UnitRepository.countActiveByTeamId` (Task 5).
- Produces: `TeamResponse.billableUnitCount : int`; the backoffice team row gains `unitCount`.

- [ ] **Step 1: Write the failing test**

In `TeamServiceTest` (create if absent): a team with 2 properties — one with 1 unit, one with 6 — reports `billableUnitCount == 7`, implicit units included. A second test covers Review Focus item 2: units under a **soft-deleted** property are excluded from the count.

- [ ] **Step 2: Run to verify it fails, implement, run again**

Run: `cd backend && mvn test -pl buurman-core -Dtest='TeamServiceTest'`

The count delegates straight to `countActiveByTeamId`, which already joins `properties` on `deleted_at IS NULL`. No enforcement, no gating — this task exposes a number and nothing more.

- [ ] **Step 3: Commit**

```bash
make bundle-openapi
git add backend/ openapi/
git commit -m "feat: expose billableUnitCount on team and backoffice (BUUR-106)"
```

---

## Task 16: Green the whole suite

**Files:**
- Modify: whatever the failing-test list from Task 4 Step 5 still names.
- Modify: `backend/buurman-booklets/`, `backend/buurman-demo-data/`, `backend/buurman-takeout/` — only as far as needed to compile and pass. Their feature work is plan 3.

- [ ] **Step 1: Run the full backend suite**

Run: `cd backend && mvn test`

- [ ] **Step 2: Fix each remaining failure**

Work down the list from Task 4 Step 5. For modules whose features belong to plan 3 (booklets, demo data, takeout, letters), the bar here is *compiles and existing tests pass* — read dwelling values from the unit instead of the property and leave the new unit-aware output to plan 3.

- [ ] **Step 3: Confirm PropertyStatus is gone**

Run: `grep -rn "PropertyStatus" backend/ --include=*.java`

Expected: no matches. If any remain, they are unconverted call sites.

- [ ] **Step 4: Verify the full suite is green**

Run: `cd backend && mvn test`

Expected: BUILD SUCCESS. Record the test count — it should be at or above the 1,036 baseline in CLAUDE.md, plus the new tests from this plan.

- [ ] **Step 5: Verify the app boots against a migrated database**

Run: `make down-v && make dev`, wait ~30s, then `cd backend && mvn spring-boot:run -pl buurman-app -am`

Expected: Flyway applies V070 cleanly on an empty database and the app starts. Then re-run against a database that already had pre-V070 data if one is available — that is the only end-to-end check of the backfill outside the test harness.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: green the backend suite after the units model change (BUUR-106)"
```

---

## Done when

- `cd backend && mvn test` is green.
- `grep -rn "PropertyStatus" backend/ --include=*.java` returns nothing.
- A fresh `make down-v && make dev` plus app boot applies V070 without error.
- `openapi/app.yaml` contains the unit paths and `yarn generate:api` produces a client (plan 2 consumes it).
- Plan 2 (frontend) and plan 3 (exports, letters, demo data, i18n) are unblocked.
