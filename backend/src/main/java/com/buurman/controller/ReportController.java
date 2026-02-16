package com.buurman.controller;

import static com.buurman.util.FeatureFlags.REPORTS;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.ExpenseBreakdownResponse;
import com.buurman.dto.response.FinancialOverviewResponse;
import com.buurman.dto.response.IncomeTrendResponse;
import com.buurman.dto.response.OccupancyTrendResponse;
import com.buurman.dto.response.PropertyComparisonResponse;
import com.buurman.dto.response.TaxSummaryResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
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

  @ModelAttribute
  private void checkReportsEnabled(@AuthenticationPrincipal UserPrincipal principal) {
    if (featureFlagService.isDisabled(REPORTS, principal)) {
      throw new ForbiddenException("Reports feature is not available");
    }
  }

  @Operation(
      summary = "Get financial overview",
      description =
          "Get comprehensive financial summary including income, expenses, and net profit for a"
              + " period")
  @GetMapping("/financial-overview")
  public FinancialOverviewResponse getFinancialOverview(
      @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
      @RequestParam(required = false) List<UUID> propertyIds,
      @RequestParam(required = false, defaultValue = "EUR") String currency,
      @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getFinancialOverview(startDate, endDate, propertyIds, currency, principal);
  }

  @Operation(
      summary = "Get income trend",
      description = "Get income, expenses, and net profit trend for the last N months")
  @GetMapping("/charts/income-trend")
  public IncomeTrendResponse getIncomeTrend(
      @RequestParam(defaultValue = "12") int months,
      @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getIncomeTrend(months, principal);
  }

  @Operation(
      summary = "Get expense breakdown",
      description = "Get expense breakdown by category with totals and percentages")
  @GetMapping("/charts/expense-breakdown")
  public ExpenseBreakdownResponse getExpenseBreakdown(
      @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getExpenseBreakdown(startDate, endDate, principal);
  }

  @Operation(
      summary = "Get property comparison",
      description = "Compare financial performance across all properties")
  @GetMapping("/charts/property-comparison")
  public PropertyComparisonResponse getPropertyComparison(
      @RequestParam @DateTimeFormat(iso = DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DATE) LocalDate endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getPropertyComparison(startDate, endDate, principal);
  }

  @Operation(
      summary = "Get occupancy trend",
      description = "Get occupancy rate trend for the last N months")
  @GetMapping("/charts/occupancy-trend")
  public OccupancyTrendResponse getOccupancyTrend(
      @RequestParam(defaultValue = "12") int months,
      @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getOccupancyTrend(months, principal);
  }

  @Operation(
      summary = "Get tax summary",
      description = "Get annual tax summary with income, expenses, and breakdown by category")
  @GetMapping("/tax-summary")
  public TaxSummaryResponse getTaxSummary(
      @RequestParam int year, @AuthenticationPrincipal UserPrincipal principal) {

    return reportService.getTaxSummary(year, principal);
  }

  @Operation(
      summary = "Export transaction history to CSV",
      description = "Download transaction history as CSV file")
  @GetMapping("/export/transactions/csv")
  public ResponseEntity<byte[]> exportTransactionHistoryCSV(
      @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    byte[] csv =
        exportService.generateTransactionHistoryCSV(startDate, endDate, principal.getTeamId());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }

  @Operation(
      summary = "Export transaction history to PDF",
      description = "Download transaction history as PDF file")
  @GetMapping("/export/transactions/pdf")
  public ResponseEntity<byte[]> exportTransactionHistoryPDF(
      @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate endDate,
      @AuthenticationPrincipal UserPrincipal principal) {

    byte[] pdf =
        exportService.generateTransactionHistoryPDF(startDate, endDate, principal.getTeamId());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transaction-history.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }
}
