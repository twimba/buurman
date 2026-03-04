package com.buurman.controller;

import static com.buurman.util.FeatureFlags.REPORTS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Property;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.DataDateRangeResponse;
import com.buurman.dto.response.ExpenseBreakdownResponse;
import com.buurman.dto.response.FinancialOverviewResponse;
import com.buurman.dto.response.IncomeTrendResponse;
import com.buurman.dto.response.OccupancyTrendResponse;
import com.buurman.dto.response.PropertyComparisonResponse;
import com.buurman.dto.response.TaxSummaryResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.ReportsApi;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.ReportService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportsApi {

  private final ReportService reportService;
  private final ExportService exportService;
  private final FeatureFlagService featureFlagService;
  private final PropertyRepository propertyRepository;
  private final HttpServletResponse httpServletResponse;

  @ModelAttribute
  public void checkReportsEnabled() {
    if (featureFlagService.isDisabled(REPORTS, SecurityUtils.getCurrentPrincipal())) {
      throw new ForbiddenException("Reports feature is not available");
    }
  }

  @Override
  public DataDateRangeResponse getDataDateRange() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return reportService.getDataDateRange(principal);
  }

  @Override
  public FinancialOverviewResponse getFinancialOverview(
      LocalDate startDate, LocalDate endDate, List<String> propertyIdentifiers, String currency) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    List<UUID> propertyIds =
        resolvePropertyIdentifiers(propertyIdentifiers, principal.requireTeamId());
    return reportService.getFinancialOverview(startDate, endDate, propertyIds, currency, principal);
  }

  @Override
  public IncomeTrendResponse getIncomeTrend(
      Integer months, LocalDate startDate, LocalDate endDate, List<String> propertyIdentifiers) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    List<UUID> propertyIds =
        resolvePropertyIdentifiers(propertyIdentifiers, principal.requireTeamId());
    if (startDate != null && endDate != null) {
      return reportService.getIncomeTrendByDateRange(startDate, endDate, propertyIds, principal);
    }
    return reportService.getIncomeTrend(months, propertyIds, principal);
  }

  @Override
  public ExpenseBreakdownResponse getExpenseBreakdown(
      LocalDate startDate, LocalDate endDate, List<String> propertyIdentifiers) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    List<UUID> propertyIds =
        resolvePropertyIdentifiers(propertyIdentifiers, principal.requireTeamId());
    return reportService.getExpenseBreakdown(startDate, endDate, propertyIds, principal);
  }

  @Override
  public PropertyComparisonResponse getPropertyComparison(
      LocalDate startDate, LocalDate endDate, List<String> propertyIdentifiers) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    List<UUID> propertyIds =
        resolvePropertyIdentifiers(propertyIdentifiers, principal.requireTeamId());
    return reportService.getPropertyComparison(startDate, endDate, propertyIds, principal);
  }

  @Override
  public OccupancyTrendResponse getOccupancyTrend(
      Integer months, LocalDate startDate, LocalDate endDate) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    if (startDate != null && endDate != null) {
      return reportService.getOccupancyTrendByDateRange(startDate, endDate, principal);
    }
    return reportService.getOccupancyTrend(months, principal);
  }

  @Override
  public TaxSummaryResponse getTaxSummary(Integer year) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return reportService.getTaxSummary(year, principal);
  }

  @Override
  public byte[] exportTransactionHistoryCSV(LocalDate startDate, LocalDate endDate) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader(CONTENT_DISPOSITION, "attachment; filename=transactions.csv");
    httpServletResponse.setContentType("text/csv");
    return exportService.generateTransactionHistoryCSV(
        startDate, endDate, principal.requireTeamId());
  }

  @Override
  public byte[] exportTransactionHistoryPDF(LocalDate startDate, LocalDate endDate) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader(
        CONTENT_DISPOSITION, "attachment; filename=transaction-history.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generateTransactionHistoryPDF(
        startDate, endDate, principal.requireTeamId());
  }

  @SuppressWarnings("NullAway")
  private List<UUID> resolvePropertyIdentifiers(List<String> identifiers, UUID teamId) {
    if (identifiers == null || identifiers.isEmpty()) {
      return null;
    }
    return identifiers.stream()
        .map(id -> propertyRepository.getByIdentifierAndTeamId(PropertyIdentifier.of(id), teamId))
        .map(Property::getId)
        .toList();
  }
}
