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
