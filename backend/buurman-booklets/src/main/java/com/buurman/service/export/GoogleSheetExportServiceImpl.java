package com.buurman.service.export;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.service.GoogleSheetExportService;
import com.buurman.service.MetricsService;
import com.buurman.service.export.google.ContactGoogleSheetExporter;
import com.buurman.service.export.google.ContractGoogleSheetExporter;
import com.buurman.service.export.google.DepositGoogleSheetExporter;
import com.buurman.service.export.google.ExpenseGoogleSheetExporter;
import com.buurman.service.export.google.PaymentGoogleSheetExporter;
import com.buurman.service.export.google.PortfolioDashboardGoogleSheetExporter;
import com.buurman.service.export.google.PropertyDashboardGoogleSheetExporter;
import com.buurman.service.export.google.PropertyGoogleSheetExporter;
import com.buurman.service.export.google.TransactionGoogleSheetExporter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleSheetExportServiceImpl implements GoogleSheetExportService {

  private final ContactGoogleSheetExporter contactExporter;
  private final TransactionGoogleSheetExporter transactionExporter;
  private final PropertyDashboardGoogleSheetExporter propertyDashboardExporter;
  private final PortfolioDashboardGoogleSheetExporter portfolioDashboardExporter;
  private final PropertyGoogleSheetExporter propertyExporter;
  private final PaymentGoogleSheetExporter paymentExporter;
  private final DepositGoogleSheetExporter depositExporter;
  private final ExpenseGoogleSheetExporter expenseExporter;
  private final ContractGoogleSheetExporter contractExporter;
  private final MetricsService metricsService;
  private final Clock clock;

  @Override
  public GoogleSheetExport generateContactsGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title) {
    return withMetrics(
        "contacts_google_sheet", () -> contactExporter.generate(token, teamId, title));
  }

  @Override
  public GoogleSheetExport generateTransactionHistoryGoogleSheet(
      GoogleAccessToken token,
      Optional<LocalDate> startDate,
      Optional<LocalDate> endDate,
      UUID teamId,
      String title) {
    return withMetrics(
        "transaction_google_sheet",
        () -> transactionExporter.generate(token, startDate, endDate, teamId, title));
  }

  @Override
  public GoogleSheetExport generatePropertyDashboardGoogleSheet(
      GoogleAccessToken token, PropertyDashboardResponse dashboard, String title) {
    return withMetrics(
        "property_dashboard_google_sheet",
        () -> propertyDashboardExporter.generate(token, dashboard, title));
  }

  @Override
  public GoogleSheetExport generatePortfolioDashboardGoogleSheet(
      GoogleAccessToken token, PortfolioDashboardResponse dashboard, String title) {
    return withMetrics(
        "portfolio_dashboard_google_sheet",
        () -> portfolioDashboardExporter.generate(token, dashboard, title));
  }

  @Override
  public GoogleSheetExport generatePropertiesGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title) {
    return withMetrics(
        "properties_google_sheet", () -> propertyExporter.generate(token, teamId, title));
  }

  @Override
  public GoogleSheetExport generateDepositsGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title) {
    return withMetrics(
        "deposits_google_sheet", () -> depositExporter.generate(token, teamId, title));
  }

  @Override
  public GoogleSheetExport generatePaymentsGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title) {
    return withMetrics(
        "payments_google_sheet", () -> paymentExporter.generate(token, teamId, title));
  }

  @Override
  public GoogleSheetExport generateExpensesGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title) {
    return withMetrics(
        "expenses_google_sheet", () -> expenseExporter.generate(token, teamId, title));
  }

  @Override
  public GoogleSheetExport generateContractsGoogleSheet(
      GoogleAccessToken token, UUID teamId, String title) {
    return withMetrics(
        "contracts_google_sheet", () -> contractExporter.generate(token, teamId, title));
  }

  private GoogleSheetExport withMetrics(String exportType, Supplier<GoogleSheetExport> generator) {
    Instant start = clock.instant();
    try {
      GoogleSheetExport result = generator.get();
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          exportType,
          "result",
          "success");
      metricsService.incrementCounter(
          "export.generation.total", "type", exportType, "result", "success");
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
