package com.buurman.controller;

import static com.buurman.util.FeatureFlags.REPORTS;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{identifier}/dashboard")
@Tag(name = "Property Dashboard", description = "Property-level investment analytics")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyDashboardController {

  private final PropertyDashboardService dashboardService;
  private final ExportService exportService;
  private final FeatureFlagService featureFlagService;

  @ModelAttribute
  private void checkReportsEnabled(@AuthenticationPrincipal UserPrincipal principal) {
    if (featureFlagService.isDisabled(REPORTS, principal)) {
      throw new ForbiddenException("Reports feature is not available");
    }
  }

  @Operation(
      summary = "Get property investment dashboard",
      description =
          "Returns financial metrics, cash flow, equity, expense breakdown, and occupancy data")
  @GetMapping
  public PropertyDashboardResponse getDashboard(
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return dashboardService.getDashboard(identifier, principal);
  }

  @Operation(summary = "Export dashboard as PDF")
  @GetMapping("/export/pdf")
  public ResponseEntity<byte[]> exportPdf(
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    PropertyDashboardResponse dashboard = dashboardService.getDashboard(identifier, principal);
    byte[] pdf = exportService.generatePropertyDashboardPDF(dashboard);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=property-dashboard.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  @Operation(summary = "Export dashboard as CSV")
  @GetMapping("/export/csv")
  public ResponseEntity<byte[]> exportCsv(
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    PropertyDashboardResponse dashboard = dashboardService.getDashboard(identifier, principal);
    byte[] csv = exportService.generatePropertyDashboardCSV(dashboard);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=property-dashboard.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }
}
