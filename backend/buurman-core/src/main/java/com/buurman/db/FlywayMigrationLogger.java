package com.buurman.db;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs Flyway migration status on application startup. This provides clear visibility that database
 * migrations have been applied.
 */
@Component
@ConditionalOnBean(Flyway.class)
@Slf4j
@RequiredArgsConstructor
public class FlywayMigrationLogger implements ApplicationListener<ApplicationReadyEvent> {

  private final Flyway flyway;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    var info = flyway.info();
    var current = info.current();

    if (current != null) {
      log.info("=".repeat(80));
      log.info("Database Migration Status:");
      log.info("  Current version: {}", current.getVersion());
      log.info("  Description: {}", current.getDescription());
      log.info("  Applied at: {}", current.getInstalledOn());
      log.info("  Total migrations applied: {}", info.applied().length);
      log.info("  Pending migrations: {}", info.pending().length);
      log.info("=".repeat(80));
    } else {
      log.warn("No Flyway migrations have been applied yet!");
    }
  }
}
