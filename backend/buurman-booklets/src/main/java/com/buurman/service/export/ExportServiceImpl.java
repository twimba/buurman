package com.buurman.service.export;

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
import com.buurman.service.ExportService;
import com.buurman.service.MetricsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

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

  @Override
  public byte[] generateTransactionHistoryCSV(
      @Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId) {
    return withMetrics(
        "transaction_csv", () -> transactionCsvExporter.generate(startDate, endDate, teamId));
  }

  @Override
  public byte[] generateTransactionHistoryPDF(
      @Nullable LocalDate startDate, @Nullable LocalDate endDate, UUID teamId) {
    return withMetrics(
        "transaction_pdf", () -> transactionPdfExporter.generate(startDate, endDate, teamId));
  }

  @Override
  public byte[] generatePropertyBrochurePDF(PropertyIdentifier propertyIdentifier, UUID teamId) {
    return withMetrics(
        "property_brochure", () -> propertyBookletExporter.generate(propertyIdentifier, teamId));
  }

  @Override
  public byte[] generateContractReportPDF(ContractIdentifier contractIdentifier, UUID teamId) {
    return withMetrics(
        "contract_report", () -> contractBookletExporter.generate(contractIdentifier, teamId));
  }

  @Override
  public byte[] generateTenantReportPDF(TenantIdentifier tenantIdentifier, UUID teamId) {
    return withMetrics(
        "tenant_report", () -> tenantBookletExporter.generate(tenantIdentifier, teamId));
  }

  @Override
  public byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard) {
    return withMetrics(
        "property_dashboard_pdf", () -> propertyDashboardPdfExporter.generate(dashboard));
  }

  @Override
  public byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard) {
    return withMetrics(
        "property_dashboard_csv", () -> propertyDashboardCsvExporter.generate(dashboard));
  }

  @Override
  public byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard) {
    return withMetrics(
        "portfolio_dashboard_pdf", () -> portfolioDashboardPdfExporter.generate(dashboard));
  }

  @Override
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
