package com.buurman.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and recent activities")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @Operation(
      summary = "Get dashboard statistics",
      description = "Get overall statistics for the dashboard")
  @GetMapping("/stats")
  public DashboardStatsResponse getDashboardStats(
      @AuthenticationPrincipal UserPrincipal principal) {
    return dashboardService.getDashboardStats(principal.requireTeamId());
  }

  @Operation(summary = "Get recent activities", description = "Get recent audit trail activities")
  @GetMapping("/recent-activities")
  public List<RecentActivityResponse> getRecentActivities(
      @AuthenticationPrincipal UserPrincipal principal,
      @Parameter(description = "Maximum number of items to return", example = "10")
          @RequestParam(defaultValue = "10")
          int limit) {
    return dashboardService.getRecentActivities(principal.requireTeamId(), limit);
  }
}
