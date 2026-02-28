package com.buurman.controller;

import static com.buurman.util.FeatureFlags.REPORTS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{identifier}/dashboard")
@Tag(name = "Property Dashboard", description = "Property-level investment analytics")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
@RequiredArgsConstructor
public class PropertyDashboardController {

  private final PropertyDashboardService dashboardService;
  private final ExportService exportService;
  private final FeatureFlagService featureFlagService;

  @ModelAttribute
  public void checkReportsEnabled(@AuthenticationPrincipal UserPrincipal principal) {
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
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Parameter(description = "Number of months to include", example = "12") @RequestParam
          Optional<Integer> months,
      @AuthenticationPrincipal UserPrincipal principal) {
    return dashboardService.getDashboard(identifier, months.orElse(null), principal);
  }

  @Operation(summary = "Export dashboard as PDF")
  @GetMapping("/export/pdf")
  public ResponseEntity<byte[]> exportPdf(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Parameter(description = "Number of months to include", example = "12") @RequestParam
          Optional<Integer> months,
      @AuthenticationPrincipal UserPrincipal principal) {
    PropertyDashboardResponse dashboard =
        dashboardService.getDashboard(identifier, months.orElse(null), principal);
    byte[] pdf = exportService.generatePropertyDashboardPDF(dashboard);
    return ResponseEntity.ok()
        .header(
            CONTENT_DISPOSITION, "attachment; filename=property-dashboard-" + identifier + ".pdf")
        .contentType(APPLICATION_PDF)
        .body(pdf);
  }

  @Operation(summary = "Export dashboard as CSV")
  @GetMapping("/export/csv")
  public ResponseEntity<byte[]> exportCsv(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Parameter(description = "Number of months to include", example = "12") @RequestParam
          Optional<Integer> months,
      @AuthenticationPrincipal UserPrincipal principal) {
    PropertyDashboardResponse dashboard =
        dashboardService.getDashboard(identifier, months.orElse(null), principal);
    byte[] csv = exportService.generatePropertyDashboardCSV(dashboard);
    return ResponseEntity.ok()
        .header(
            CONTENT_DISPOSITION, "attachment; filename=property-dashboard-" + identifier + ".csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }
}
