package com.buurman.db;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Logs Flyway migration status on application startup.
 * This provides clear visibility that database migrations have been applied.
 */
@Component
@ConditionalOnBean(Flyway.class)
public class FlywayMigrationLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FlywayMigrationLogger.class);

    private final Flyway flyway;

    @Autowired
    public FlywayMigrationLogger(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var info = flyway.info();
        var current = info.current();

        if (current != null) {
            logger.info("=".repeat(80));
            logger.info("Database Migration Status:");
            logger.info("  Current version: {}", current.getVersion());
            logger.info("  Description: {}", current.getDescription());
            logger.info("  Applied at: {}", current.getInstalledOn());
            logger.info("  Total migrations applied: {}", info.applied().length);
            logger.info("  Pending migrations: {}", info.pending().length);
            logger.info("=".repeat(80));
        } else {
            logger.warn("No Flyway migrations have been applied yet!");
        }
    }
}
