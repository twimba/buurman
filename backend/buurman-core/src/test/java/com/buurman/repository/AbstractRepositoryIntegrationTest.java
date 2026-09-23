package com.buurman.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
 * Base class for repository integration tests. Starts a single shared PostgreSQL container, runs
 * all Flyway migrations once, and provides a DSLContext + fixed Clock. Each test gets a clean
 * database via truncation.
 */
abstract class AbstractRepositoryIntegrationTest {

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>("postgres:18-alpine")
          .withDatabaseName("buurman_test")
          .withUsername("buurman")
          .withPassword("buurman");

  protected static DSLContext dsl;
  protected static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  protected static final UUID TEAM_A_ID = UUID.randomUUID();
  protected static final UUID TEAM_B_ID = UUID.randomUUID();
  protected static final UUID USER_ID = UUID.randomUUID();

  static {
    PG.start();

    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(PG.getJdbcUrl());
    ds.setUser(PG.getUsername());
    ds.setPassword(PG.getPassword());

    Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();

    dsl = DSL.using((DataSource) ds, SQLDialect.POSTGRES);
  }

  @BeforeEach
  void cleanDatabase() {
    // Delete in reverse FK order to avoid constraint violations
    dsl.deleteFrom(DSL.table("payment_reminders")).execute();
    dsl.deleteFrom(DSL.table("payment_receivals")).execute();
    dsl.deleteFrom(DSL.table("payments")).execute();
    dsl.deleteFrom(DSL.table("contract_rent_periods")).execute();
    dsl.deleteFrom(DSL.table("contract_rent_components")).execute();
    dsl.deleteFrom(DSL.table("contract_payment_instructions")).execute();
    dsl.deleteFrom(DSL.table("contract_parties")).execute();
    dsl.deleteFrom(DSL.table("contract_extensions")).execute();
    dsl.deleteFrom(DSL.table("contracts")).execute();
    dsl.deleteFrom(DSL.table("contact_tags")).execute();
    dsl.deleteFrom(DSL.table("contact_relationships")).execute();
    dsl.deleteFrom(DSL.table("contact_notes")).execute();
    dsl.deleteFrom(DSL.table("contact_addresses")).execute();
    dsl.deleteFrom(DSL.table("property_contact_history")).execute();
    dsl.deleteFrom(DSL.table("contacts")).execute();
    dsl.deleteFrom(DSL.table("property_amenities")).execute();
    dsl.deleteFrom(DSL.table("property_fees")).execute();
    dsl.deleteFrom(DSL.table("property_occupancy_periods")).execute();
    dsl.deleteFrom(DSL.table("property_outdoor_areas")).execute();
    dsl.deleteFrom(DSL.table("property_valuations")).execute();
    dsl.deleteFrom(DSL.table("property_taxes")).execute();
    dsl.deleteFrom(DSL.table("property_insurances")).execute();
    dsl.deleteFrom(DSL.table("property_financings")).execute();
    dsl.deleteFrom(DSL.table("property_acquisitions")).execute();
    dsl.deleteFrom(DSL.table("documents")).execute();
    dsl.deleteFrom(DSL.table("photos")).execute();
    dsl.deleteFrom(DSL.table("properties")).execute();
    dsl.deleteFrom(DSL.table("team_preferences")).execute();
    dsl.deleteFrom(DSL.table("team_members")).execute();
    dsl.deleteFrom(DSL.table("teams"))
        .where(DSL.field("id").notEqual(UUID.fromString("00000000-0000-0000-0000-000000000001")))
        .execute();
    dsl.deleteFrom(DSL.table("users"))
        .where(DSL.field("id").notEqual(UUID.fromString("00000000-0000-0000-0000-000000000001")))
        .execute();

    // Insert shared test fixtures
    TestDataHelper.insertUser(dsl, USER_ID);
    TestDataHelper.insertTeam(dsl, TEAM_A_ID, "Team A", USER_ID);
    TestDataHelper.insertTeam(dsl, TEAM_B_ID, "Team B", USER_ID);
  }
}
