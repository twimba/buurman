package com.buurman.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.service.export.ContractBookletExporter;
import com.buurman.service.export.PortfolioDashboardCsvExporter;
import com.buurman.service.export.PortfolioDashboardPdfExporter;
import com.buurman.service.export.PropertyBookletExporter;
import com.buurman.service.export.PropertyDashboardCsvExporter;
import com.buurman.service.export.PropertyDashboardPdfExporter;
import com.buurman.service.export.TenantBookletExporter;
import com.buurman.service.export.TransactionCsvExporter;
import com.buurman.service.export.TransactionPdfExporter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportService {

  private final TransactionCsvExporter transactionCsvExporter;
  private final TransactionPdfExporter transactionPdfExporter;
  private final PropertyBookletExporter propertyBookletExporter;
  private final ContractBookletExporter contractBookletExporter;
  private final TenantBookletExporter tenantBookletExporter;
  private final PropertyDashboardPdfExporter propertyDashboardPdfExporter;
  private final PropertyDashboardCsvExporter propertyDashboardCsvExporter;
  private final PortfolioDashboardPdfExporter portfolioDashboardPdfExporter;
  private final PortfolioDashboardCsvExporter portfolioDashboardCsvExporter;
  private final MetricsService metricsService;
  private final Clock clock;

  public byte[] generateTransactionHistoryCSV(
      @Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId) {
    return withMetrics(
        "transaction_csv", () -> transactionCsvExporter.generate(startDate, endDate, teamId));
  }

  public byte[] generateTransactionHistoryPDF(
      @Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId) {
    return withMetrics(
        "transaction_pdf", () -> transactionPdfExporter.generate(startDate, endDate, teamId));
  }

  public byte[] generatePropertyBrochurePDF(PropertyIdentifier propertyIdentifier, UUID teamId) {
    return withMetrics(
        "property_brochure", () -> propertyBookletExporter.generate(propertyIdentifier, teamId));
  }

  public byte[] generateContractReportPDF(ContractIdentifier contractIdentifier, UUID teamId) {
    return withMetrics(
        "contract_report", () -> contractBookletExporter.generate(contractIdentifier, teamId));
  }

  public byte[] generateTenantReportPDF(TenantIdentifier tenantIdentifier, UUID teamId) {
    return withMetrics(
        "tenant_report", () -> tenantBookletExporter.generate(tenantIdentifier, teamId));
  }

  public byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard) {
    return withMetrics(
        "property_dashboard_pdf", () -> propertyDashboardPdfExporter.generate(dashboard));
  }

  public byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard) {
    return withMetrics(
        "property_dashboard_csv", () -> propertyDashboardCsvExporter.generate(dashboard));
  }

  public byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard) {
    return withMetrics(
        "portfolio_dashboard_pdf", () -> portfolioDashboardPdfExporter.generate(dashboard));
  }

  public byte[] generatePortfolioDashboardCSV(PortfolioDashboardResponse dashboard) {
    return withMetrics(
        "portfolio_dashboard_csv", () -> portfolioDashboardCsvExporter.generate(dashboard));
  }

  private byte[] withMetrics(String exportType, Supplier<byte[]> generator) {
    Instant start = clock.instant();
    try {
      byte[] result = generator.get();
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          exportType,
          "result",
          "success");
      metricsService.incrementCounter(
          "export.generation.total", "type", exportType, "result", "success");
      metricsService.recordHistogram("export.size.bytes", result.length, "type", exportType);
      return result;
    } catch (Exception e) {
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          exportType,
          "result",
          "failure");
      metricsService.incrementCounter(
          "export.generation.total", "type", exportType, "result", "failure");
      throw e;
    }
  }
}
