package com.buurman.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.slf4j.Logger;

import static org.jooq.impl.DSL.*;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

import static com.buurman.jooq.generated.Tables.*;

@Service
public class DatabaseMetricsService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMetricsService.class);
    private static final String PREFIX = "buurman.";

    private final DSLContext dsl;

    /** Subquery selecting team IDs where settings contains "demoData": true */
    private static final Condition NOT_DEMO_TEAM = TEAMS.SETTINGS.cast(String.class)
            .notContains("\"demoData\"");

    private final AtomicLong propertiesCount = new AtomicLong();
    private final AtomicLong contractsCount = new AtomicLong();
    private final AtomicLong tenantsCount = new AtomicLong();
    private final AtomicLong paymentsCount = new AtomicLong();
    private final AtomicLong teamsCount = new AtomicLong();
    private final AtomicLong expensesCount = new AtomicLong();

    private final MultiGauge contractsByStatus;
    private final MultiGauge paymentsByStatus;

    public DatabaseMetricsService(DSLContext dsl, MeterRegistry registry) {
        this.dsl = dsl;

        Gauge.builder(PREFIX + "properties.count", propertiesCount, AtomicLong::doubleValue)
                .description("Total active properties").register(registry);
        Gauge.builder(PREFIX + "contracts.count", contractsCount, AtomicLong::doubleValue)
                .description("Total active contracts").register(registry);
        Gauge.builder(PREFIX + "tenants.count", tenantsCount, AtomicLong::doubleValue)
                .description("Total active tenants").register(registry);
        Gauge.builder(PREFIX + "payments.count", paymentsCount, AtomicLong::doubleValue)
                .description("Total active payments").register(registry);
        Gauge.builder(PREFIX + "teams.count", teamsCount, AtomicLong::doubleValue)
                .description("Total teams").register(registry);
        Gauge.builder(PREFIX + "expenses.count", expensesCount, AtomicLong::doubleValue)
                .description("Total active expenses").register(registry);

        contractsByStatus = MultiGauge.builder(PREFIX + "contracts.by.status")
                .description("Contracts grouped by status").register(registry);
        paymentsByStatus = MultiGauge.builder(PREFIX + "payments.by.status")
                .description("Payments grouped by status").register(registry);

        refreshCounts();
    }

    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    public void refreshCounts() {
        try {
            var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);

            propertiesCount.set(dsl.fetchCount(PROPERTIES,
                    PROPERTIES.DELETED_AT.isNull().and(PROPERTIES.TEAM_ID.in(nonDemoTeamIds))));
            contractsCount.set(dsl.fetchCount(CONTRACTS,
                    CONTRACTS.DELETED_AT.isNull().and(CONTRACTS.TEAM_ID.in(nonDemoTeamIds))));
            tenantsCount.set(dsl.fetchCount(TENANTS,
                    TENANTS.DELETED_AT.isNull().and(TENANTS.TEAM_ID.in(nonDemoTeamIds))));
            paymentsCount.set(dsl.fetchCount(PAYMENTS,
                    PAYMENTS.DELETED_AT.isNull().and(PAYMENTS.TEAM_ID.in(nonDemoTeamIds))));
            teamsCount.set(dsl.fetchCount(TEAMS, NOT_DEMO_TEAM));
            expensesCount.set(dsl.fetchCount(EXPENSES,
                    EXPENSES.DELETED_AT.isNull().and(EXPENSES.TEAM_ID.in(nonDemoTeamIds))));

            refreshContractsByStatus();
            refreshPaymentsByStatus();
        } catch (Exception e) {
            log.warn("Failed to refresh database metrics", e);
        }
    }

    private void refreshContractsByStatus() {
        var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
        Result<Record2<String, Integer>> result = dsl
                .select(CONTRACTS.STATUS, count())
                .from(CONTRACTS)
                .where(CONTRACTS.DELETED_AT.isNull())
                .and(CONTRACTS.TEAM_ID.in(nonDemoTeamIds))
                .groupBy(CONTRACTS.STATUS)
                .fetch();

        var rows = result.stream()
                .<MultiGauge.Row<?>>map(r -> MultiGauge.Row.of(Tags.of("status", r.value1()), r.value2()))
                .toList();
        contractsByStatus.register(rows, true);
    }

    private void refreshPaymentsByStatus() {
        var nonDemoTeamIds = select(TEAMS.ID).from(TEAMS).where(NOT_DEMO_TEAM);
        Result<Record2<String, Integer>> result = dsl
                .select(PAYMENTS.STATUS, count())
                .from(PAYMENTS)
                .where(PAYMENTS.DELETED_AT.isNull())
                .and(PAYMENTS.TEAM_ID.in(nonDemoTeamIds))
                .groupBy(PAYMENTS.STATUS)
                .fetch();

        var rows = result.stream()
                .<MultiGauge.Row<?>>map(r -> MultiGauge.Row.of(Tags.of("status", r.value1()), r.value2()))
                .toList();
        paymentsByStatus.register(rows, true);
    }
}
