package com.buurman.service.export;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
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
  private final ContactBookletExporter contactBookletExporter;
  private final PropertyDashboardPdfExporter propertyDashboardPdfExporter;
  private final PropertyDashboardCsvExporter propertyDashboardCsvExporter;
  private final PortfolioDashboardPdfExporter portfolioDashboardPdfExporter;
  private final PortfolioDashboardCsvExporter portfolioDashboardCsvExporter;
  private final TransactionExcelExporter transactionExcelExporter;
  private final PropertyDashboardExcelExporter propertyDashboardExcelExporter;
  private final PortfolioDashboardExcelExporter portfolioDashboardExcelExporter;
  private final ContactCsvExporter contactCsvExporter;
  private final ContactExcelExporter contactExcelExporter;
  private final MetricsService metricsService;
  private final Clock clock;

  @Override
  public byte[] generateTransactionHistoryCSV(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    return withMetrics(
        "transaction_csv", () -> transactionCsvExporter.generate(startDate, endDate, teamId));
  }

  @Override
  public byte[] generateTransactionHistoryPDF(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    return withMetrics(
        "transaction_pdf", () -> transactionPdfExporter.generate(startDate, endDate, teamId));
  }

  @Override
  public byte[] generatePropertyBrochurePDF(
      PropertyIdentifier propertyIdentifier, UUID teamId, Locale locale) {
    return withMetrics(
        "property_brochure",
        () -> propertyBookletExporter.generate(propertyIdentifier, teamId, locale));
  }

  @Override
  public byte[] generateContractReportPDF(
      ContractIdentifier contractIdentifier, UUID teamId, Locale locale) {
    return withMetrics(
        "contract_report",
        () -> contractBookletExporter.generate(contractIdentifier, teamId, locale));
  }

  @Override
  public byte[] generateContactReportPDF(
      ContactIdentifier contactIdentifier, UUID teamId, Locale locale) {
    return withMetrics(
        "contact_report", () -> contactBookletExporter.generate(contactIdentifier, teamId, locale));
  }

  @Override
  public byte[] generatePropertyDashboardPDF(PropertyDashboardResponse dashboard, UUID teamId) {
    return withMetrics(
        "property_dashboard_pdf", () -> propertyDashboardPdfExporter.generate(dashboard, teamId));
  }

  @Override
  public byte[] generatePropertyDashboardCSV(PropertyDashboardResponse dashboard) {
    return withMetrics(
        "property_dashboard_csv", () -> propertyDashboardCsvExporter.generate(dashboard));
  }

  @Override
  public byte[] generatePortfolioDashboardPDF(PortfolioDashboardResponse dashboard, UUID teamId) {
    return withMetrics(
        "portfolio_dashboard_pdf", () -> portfolioDashboardPdfExporter.generate(dashboard, teamId));
  }

  @Override
  public byte[] generatePortfolioDashboardCSV(PortfolioDashboardResponse dashboard) {
    return withMetrics(
        "portfolio_dashboard_csv", () -> portfolioDashboardCsvExporter.generate(dashboard));
  }

  @Override
  public byte[] generateTransactionHistoryExcel(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    return withMetrics(
        "transaction_excel", () -> transactionExcelExporter.generate(startDate, endDate, teamId));
  }

  @Override
  public byte[] generatePropertyDashboardExcel(PropertyDashboardResponse dashboard) {
    return withMetrics(
        "property_dashboard_excel", () -> propertyDashboardExcelExporter.generate(dashboard));
  }

  @Override
  public byte[] generatePortfolioDashboardExcel(PortfolioDashboardResponse dashboard) {
    return withMetrics(
        "portfolio_dashboard_excel", () -> portfolioDashboardExcelExporter.generate(dashboard));
  }

  @Override
  public byte[] generateContactsCSV(UUID teamId) {
    return withMetrics("contacts_csv", () -> contactCsvExporter.generate(teamId));
  }

  @Override
  public byte[] generateContactsExcel(UUID teamId) {
    return withMetrics("contacts_excel", () -> contactExcelExporter.generate(teamId));
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
