package com.buurman.controller;

import static com.buurman.util.FeatureFlags.REPORTS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.util.Optional;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.PortfolioDashboardApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PortfolioDashboardService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PortfolioDashboardController implements PortfolioDashboardApi {

  private final PortfolioDashboardService dashboardService;
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
  public PortfolioDashboardResponse getPortfolioDashboard(Optional<Integer> months) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dashboardService.getPortfolioDashboard(months, principal);
  }

  @Override
  public byte[] exportPortfolioPdf(Optional<Integer> months) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PortfolioDashboardResponse dashboard =
        dashboardService.getPortfolioDashboard(months, principal);
    httpServletResponse.setHeader(
        CONTENT_DISPOSITION, "attachment; filename=portfolio-dashboard.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generatePortfolioDashboardPDF(dashboard);
  }

  @Override
  public byte[] exportPortfolioCsv(Optional<Integer> months) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PortfolioDashboardResponse dashboard =
        dashboardService.getPortfolioDashboard(months, principal);
    httpServletResponse.setHeader(
        CONTENT_DISPOSITION, "attachment; filename=portfolio-dashboard.csv");
    httpServletResponse.setContentType("text/csv");
    return exportService.generatePortfolioDashboardCSV(dashboard);
  }
}
