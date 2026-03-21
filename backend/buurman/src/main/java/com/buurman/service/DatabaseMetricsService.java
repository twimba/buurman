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

  private final MultiGauge contractsByStatus;
  private final MultiGauge paymentsByStatus;
  private final MultiGauge propertiesByStatus;

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

    contractsByStatus =
        MultiGauge.builder(PREFIX + "contracts.by.status")
            .description("Contracts grouped by status")
            .register(registry);
    paymentsByStatus =
        MultiGauge.builder(PREFIX + "payments.by.status")
            .description("Payments grouped by status")
            .register(registry);
    propertiesByStatus =
        MultiGauge.builder(PREFIX + "properties.by.status")
            .description("Properties grouped by status")
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

      refreshContractsByStatus();
      refreshPaymentsByStatus();
      refreshPropertiesByStatus();
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

  private void refreshPropertiesByStatus() {
    var rows =
        metricsRepository.countPropertiesByStatus().stream()
            .<MultiGauge.Row<?>>map(
                lc -> MultiGauge.Row.of(Tags.of("status", lc.label()), lc.count()))
            .toList();
    propertiesByStatus.register(rows, true);
  }
}
