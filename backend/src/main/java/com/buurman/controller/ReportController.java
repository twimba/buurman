package com.buurman.controller;

import static com.buurman.util.FeatureFlags.REPORTS;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Property;
import com.buurman.dto.response.DataDateRangeResponse;
import com.buurman.dto.response.ExpenseBreakdownResponse;
import com.buurman.dto.response.FinancialOverviewResponse;
import com.buurman.dto.response.IncomeTrendResponse;
import com.buurman.dto.response.OccupancyTrendResponse;
import com.buurman.dto.response.PropertyComparisonResponse;
import com.buurman.dto.response.TaxSummaryResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Financial reporting and analytics")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;
  private final ExportService exportService;
  private final FeatureFlagService featureFlagService;
  private final PropertyRepository propertyRepository;

  @ModelAttribute
  public void checkReportsEnabled(@AuthenticationPrincipal UserPrincipal principal) {
    if (featureFlagService.isDisabled(REPORTS, principal)) {
      throw new ForbiddenException("Reports feature is not available");
    }
  }

  @Operation(
      summary = "Get data date range",
      description = "Get the earliest date across all financial data for the team")
  @GetMapping("/date-range")
  public DataDateRangeResponse getDataDateRange(@AuthenticationPrincipal UserPrincipal principal) {
    return reportService.getDataDateRange(principal);
  }

  @Operation(
      summary = "Get financial overview",
      description =
          "Get comprehensive financial summary including income, expenses, and net profit for a"
              + " period")
  @GetMapping("/financial-overview")
  public FinancialOverviewResponse getFinancialOverview(
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          LocalDate startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          LocalDate endDate,
      @Parameter(description = "Property ULID identifiers to filter by") @RequestParam
          Optional<List<String>> propertyIdentifiers,
      @Parameter(description = "Currency code", example = "EUR") @RequestParam
          Optional<String> currency,
      @AuthenticationPrincipal UserPrincipal principal) {

    List<UUID> propertyIds =
        propertyIdentifiers
            .flatMap(ids -> resolvePropertyIdentifiers(ids, principal.requireTeamId()))
            .orElse(null);
    return reportService.getFinancialOverview(
        startDate, endDate, propertyIds, currency.orElse(null), principal);
  }

  @Operation(
      summary = "Get income trend",
      description = "Get income, expenses, and net profit trend for a date range or last N months")
  @GetMapping("/charts/income-trend")
  public IncomeTrendResponse getIncomeTrend(
      @Parameter(description = "Number of months to look back", example = "12")
          @RequestParam(defaultValue = "12")
          int months,
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> endDate,
      @Parameter(description = "Property ULID identifiers to filter by") @RequestParam
          Optional<List<String>> propertyIdentifiers,
      @AuthenticationPrincipal UserPrincipal principal) {

    List<UUID> propertyIds =
        propertyIdentifiers
            .flatMap(ids -> resolvePropertyIdentifiers(ids, principal.requireTeamId()))
            .orElse(null);
    if (startDate.isPresent() && endDate.isPresent()) {
      return reportService.getIncomeTrendByDateRange(
          startDate.get(), endDate.get(), propertyIds, principal);
    }
    return reportService.getIncomeTrend(months, propertyIds, principal);
  }

  @Operation(
      summary = "Get expense breakdown",
      description = "Get expense breakdown by category with totals and percentages")
  @GetMapping("/charts/expense-breakdown")
  public ExpenseBreakdownResponse getExpenseBreakdown(
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          LocalDate startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          LocalDate endDate,
      @Parameter(description = "Property ULID identifiers to filter by") @RequestParam
          Optional<List<String>> propertyIdentifiers,
      @AuthenticationPrincipal UserPrincipal principal) {

    List<UUID> propertyIds =
        propertyIdentifiers
            .flatMap(ids -> resolvePropertyIdentifiers(ids, principal.requireTeamId()))
            .orElse(null);
    return reportService.getExpenseBreakdown(startDate, endDate, propertyIds, principal);
  }

  @Operation(
      summary = "Get property comparison",
      description = "Compare financial performance across all properties")
  @GetMapping("/charts/property-comparison")
  public PropertyComparisonResponse getPropertyComparison(
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          LocalDate startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          LocalDate endDate,
      @Parameter(description = "Property ULID identifiers to filter by") @RequestParam
          Optional<List<String>> propertyIdentifiers,
      @AuthenticationPrincipal UserPrincipal principal) {

    List<UUID> propertyIds =
        propertyIdentifiers
            .flatMap(ids -> resolvePropertyIdentifiers(ids, principal.requireTeamId()))
            .orElse(null);
    return reportService.getPropertyComparison(startDate, endDate, propertyIds, principal);
  }

  @Operation(
      summary = "Get occupancy trend",
      description = "Get occupancy rate trend for a date range or last N months")
  @GetMapping("/charts/occupancy-trend")
  public OccupancyTrendResponse getOccupancyTrend(
      @Parameter(description = "Number of months to look back", example = "12")
          @RequestParam(defaultValue = "12")
          int months,
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    if (startDate.isPresent() && endDate.isPresent()) {
      return reportService.getOccupancyTrendByDateRange(startDate.get(), endDate.get(), principal);
    }
    return reportService.getOccupancyTrend(months, principal);
  }

  @Operation(
      summary = "Get tax summary",
      description = "Get annual tax summary with income, expenses, and breakdown by category")
  @GetMapping("/tax-summary")
  public TaxSummaryResponse getTaxSummary(
      @Parameter(description = "Fiscal year", example = "2026") @RequestParam int year,
      @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getTaxSummary(year, principal);
  }

  @Operation(
      summary = "Export transaction history to CSV",
      description = "Download transaction history as CSV file")
  @GetMapping("/export/transactions/csv")
  public ResponseEntity<byte[]> exportTransactionHistoryCSV(
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    byte[] csv =
        exportService.generateTransactionHistoryCSV(
            startDate.orElse(null), endDate.orElse(null), principal.requireTeamId());

    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }

  @Operation(
      summary = "Export transaction history to PDF",
      description = "Download transaction history as PDF file")
  @GetMapping("/export/transactions/pdf")
  public ResponseEntity<byte[]> exportTransactionHistoryPDF(
      @Parameter(description = "Start date filter (inclusive)", example = "2026-01-01")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> startDate,
      @Parameter(description = "End date filter (inclusive)", example = "2026-12-31")
          @RequestParam
          @DateTimeFormat(iso = DATE)
          Optional<LocalDate> endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    byte[] pdf =
        exportService.generateTransactionHistoryPDF(
            startDate.orElse(null), endDate.orElse(null), principal.requireTeamId());

    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment; filename=transaction-history.pdf")
        .contentType(APPLICATION_PDF)
        .body(pdf);
  }

  private Optional<List<UUID>> resolvePropertyIdentifiers(List<String> identifiers, UUID teamId) {
    if (identifiers.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        identifiers.stream()
            .map(id -> propertyRepository.getByIdentifierAndTeamId(id, teamId))
            .map(Property::getId)
            .toList());
  }
}
