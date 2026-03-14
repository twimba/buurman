package com.buurman.controller;

import static com.buurman.util.FeatureFlags.EXCEL_EXPORT;
import static com.buurman.util.FeatureFlags.REPORTS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.util.Optional;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.PropertyDashboardApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyDashboardController implements PropertyDashboardApi {

  private final PropertyDashboardService dashboardService;
  private final ExportService exportService;
  private final FeatureFlagService featureFlagService;
  private final HttpServletResponse httpServletResponse;

  @ModelAttribute
  public void checkReportsEnabled() {
    if (featureFlagService.isDisabled(REPORTS, SecurityUtils.getCurrentPrincipal())) {
      throw new ForbiddenException("Reports feature is not available");
    }
  }

  @Override
  public PropertyDashboardResponse getDashboard(
      PropertyIdentifier identifier, Optional<Integer> months) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dashboardService.getDashboard(identifier, months.orElse(null), principal);
  }

  @Override
  public byte[] exportPdf(PropertyIdentifier identifier, Optional<Integer> months) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PropertyDashboardResponse dashboard =
        dashboardService.getDashboard(identifier, months.orElse(null), principal);
    httpServletResponse.setHeader(
        CONTENT_DISPOSITION, "attachment; filename=property-dashboard-" + identifier + ".pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generatePropertyDashboardPDF(dashboard);
  }

  @Override
  public byte[] exportCsv(PropertyIdentifier identifier, Optional<Integer> months) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PropertyDashboardResponse dashboard =
        dashboardService.getDashboard(identifier, months.orElse(null), principal);
    httpServletResponse.setHeader(
        CONTENT_DISPOSITION, "attachment; filename=property-dashboard-" + identifier + ".csv");
    httpServletResponse.setContentType("text/csv");
    return exportService.generatePropertyDashboardCSV(dashboard);
  }

  @Override
  public byte[] exportExcel(PropertyIdentifier identifier, Optional<Integer> months) {
    if (featureFlagService.isDisabled(EXCEL_EXPORT, SecurityUtils.getCurrentPrincipal())) {
      throw new ForbiddenException("Excel export feature is not available");
    }
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PropertyDashboardResponse dashboard =
        dashboardService.getDashboard(identifier, months.orElse(null), principal);
    httpServletResponse.setHeader(
        CONTENT_DISPOSITION, "attachment; filename=property-dashboard-" + identifier + ".xlsx");
    httpServletResponse.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    return exportService.generatePropertyDashboardExcel(dashboard);
  }
}
