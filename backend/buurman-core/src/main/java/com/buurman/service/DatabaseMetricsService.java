package com.buurman.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.buurman.repository.DatabaseMetricsRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DatabaseMetricsService {

  private static final String PREFIX = "buurman.";

  private final DatabaseMetricsRepository metricsRepository;

  private final AtomicLong propertiesCount = new AtomicLong();
  private final AtomicLong contractsCount = new AtomicLong();
  private final AtomicLong contactsCount = new AtomicLong();
  private final AtomicLong paymentsCount = new AtomicLong();
  private final AtomicLong teamsCount = new AtomicLong();
  private final AtomicLong expensesCount = new AtomicLong();
  private final AtomicLong unitsCount = new AtomicLong();
  private final AtomicLong allocationsCount = new AtomicLong();

  private final MultiGauge contractsByStatus;
  private final MultiGauge paymentsByStatus;
  private final MultiGauge unitsByStatus;

  public DatabaseMetricsService(
      DatabaseMetricsRepository metricsRepository, MeterRegistry registry) {
    this.metricsRepository = metricsRepository;

    Gauge.builder(PREFIX + "properties.count", propertiesCount, AtomicLong::doubleValue)
        .description("Total active properties")
        .register(registry);
    Gauge.builder(PREFIX + "contracts.count", contractsCount, AtomicLong::doubleValue)
        .description("Total active contracts")
        .register(registry);
    Gauge.builder(PREFIX + "contacts.count", contactsCount, AtomicLong::doubleValue)
        .description("Total active contacts")
        .register(registry);
    Gauge.builder(PREFIX + "payments.count", paymentsCount, AtomicLong::doubleValue)
        .description("Total active payments")
        .register(registry);
    Gauge.builder(PREFIX + "teams.count", teamsCount, AtomicLong::doubleValue)
        .description("Total teams")
        .register(registry);
    Gauge.builder(PREFIX + "expenses.count", expensesCount, AtomicLong::doubleValue)
        .description("Total active expenses")
        .register(registry);
    // BUUR-106 wave3c Important 7: the entity the product now bills on had no metric at all.
    Gauge.builder(PREFIX + "units.count", unitsCount, AtomicLong::doubleValue)
        .description("Total active units")
        .register(registry);
    Gauge.builder(PREFIX + "allocations.count", allocationsCount, AtomicLong::doubleValue)
        .description("Total active expense allocations")
        .register(registry);

    contractsByStatus =
        MultiGauge.builder(PREFIX + "contracts.by.status")
            .description("Contracts grouped by status")
            .register(registry);
    paymentsByStatus =
        MultiGauge.builder(PREFIX + "payments.by.status")
            .description("Payments grouped by status")
            .register(registry);
    // Replaces the retired "properties.by.status" gauge (BUUR-106 wave3c Important 7): that series
    // was grouping by units.status while still being registered/described as a property breakdown,
    // so sum(by_status) equalled buurman_properties_count before this deploy and silently stopped
    // matching it after -- a deploy artifact that read as a business event. Named for what it
    // actually measures.
    unitsByStatus =
        MultiGauge.builder(PREFIX + "units.by.status")
            .description("Units grouped by status")
            .register(registry);

    refreshCounts();
  }

  public void refreshCounts() {
    try {
      propertiesCount.set(metricsRepository.countProperties());
      contractsCount.set(metricsRepository.countContracts());
      contactsCount.set(metricsRepository.countContacts());
      paymentsCount.set(metricsRepository.countPayments());
      teamsCount.set(metricsRepository.countTeams());
      expensesCount.set(metricsRepository.countExpenses());
      unitsCount.set(metricsRepository.countUnits());
      allocationsCount.set(metricsRepository.countAllocations());

      refreshContractsByStatus();
      refreshPaymentsByStatus();
      refreshUnitsByStatus();
    } catch (Exception e) {
      log.warn("Failed to refresh database metrics", e);
    }
  }

  private void refreshContractsByStatus() {
    var rows =
        metricsRepository.countContractsByStatus().stream()
            .<MultiGauge.Row<?>>map(
                lc -> MultiGauge.Row.of(Tags.of("status", lc.label()), lc.count()))
            .toList();
    contractsByStatus.register(rows, true);
  }

  private void refreshPaymentsByStatus() {
    var rows =
        metricsRepository.countPaymentsByStatus().stream()
            .<MultiGauge.Row<?>>map(
                lc -> MultiGauge.Row.of(Tags.of("status", lc.label()), lc.count()))
            .toList();
    paymentsByStatus.register(rows, true);
  }

  private void refreshUnitsByStatus() {
    var rows =
        metricsRepository.countUnitsByStatus().stream()
            .<MultiGauge.Row<?>>map(
                lc -> MultiGauge.Row.of(Tags.of("status", lc.label()), lc.count()))
            .toList();
    unitsByStatus.register(rows, true);
  }
}
