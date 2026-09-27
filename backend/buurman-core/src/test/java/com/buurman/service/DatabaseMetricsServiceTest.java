package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.LabelCount;
import com.buurman.repository.DatabaseMetricsRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * BUUR-106 wave3c Important 7: {@code countPropertiesByStatus} grouped by {@code units.status}
 * while still registered as {@code buurman.properties.by.status}, so {@code sum(by_status)}
 * equalled {@code buurman_properties_count} before this deploy and silently stopped matching it
 * after -- a deploy artifact that read as a business event. And there was no metric at all for
 * units, the entity the product now bills on.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseMetricsService")
class DatabaseMetricsServiceTest {

  @Mock private DatabaseMetricsRepository metricsRepository;

  private SimpleMeterRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    when(metricsRepository.countProperties()).thenReturn(3L);
    when(metricsRepository.countContracts()).thenReturn(5L);
    when(metricsRepository.countContacts()).thenReturn(7L);
    when(metricsRepository.countPayments()).thenReturn(11L);
    when(metricsRepository.countTeams()).thenReturn(2L);
    when(metricsRepository.countExpenses()).thenReturn(13L);
    when(metricsRepository.countUnits()).thenReturn(17L);
    when(metricsRepository.countAllocations()).thenReturn(19L);
    when(metricsRepository.countContractsByStatus()).thenReturn(List.of());
    when(metricsRepository.countPaymentsByStatus()).thenReturn(List.of());
    when(metricsRepository.countUnitsByStatus())
        .thenReturn(List.of(new LabelCount("VACANT", 4), new LabelCount("OCCUPIED", 13)));
  }

  @Test
  @DisplayName("registers buurman.units.count, backed by the entity the product bills on")
  void registersUnitsCount() {
    new DatabaseMetricsService(metricsRepository, registry);

    assertThat(registry.get("buurman.units.count").gauge().value()).isEqualTo(17.0);
  }

  @Test
  @DisplayName("registers buurman.allocations.count")
  void registersAllocationsCount() {
    new DatabaseMetricsService(metricsRepository, registry);

    assertThat(registry.get("buurman.allocations.count").gauge().value()).isEqualTo(19.0);
  }

  @Test
  @DisplayName(
      "registers buurman.units.by.status from countUnitsByStatus, not properties.by.status")
  void registersUnitsByStatusNotPropertiesByStatus() {
    new DatabaseMetricsService(metricsRepository, registry);

    double vacant = registry.get("buurman.units.by.status").tag("status", "VACANT").gauge().value();
    double occupied =
        registry.get("buurman.units.by.status").tag("status", "OCCUPIED").gauge().value();
    assertThat(vacant).isEqualTo(4.0);
    assertThat(occupied).isEqualTo(13.0);

    // The old, mis-labelled series must not still be registered under either name.
    assertThat(registry.find("buurman.properties.by.status").gauge()).isNull();
  }
}
