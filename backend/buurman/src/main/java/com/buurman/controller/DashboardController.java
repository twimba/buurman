package com.buurman.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.api.DashboardApi;
import com.buurman.generated.model.UpcomingRenewalResponse;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

  private final DashboardService dashboardService;

  @Override
  public DashboardStatsResponse getDashboardStats() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dashboardService.getDashboardStats(principal.requireTeamId());
  }

  @Override
  public List<RecentActivityResponse> getRecentActivities(Optional<Integer> limit) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dashboardService.getRecentActivities(principal.requireTeamId(), limit.orElse(10));
  }

  @Override
  public List<UpcomingRenewalResponse> getUpcomingRenewals() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dashboardService.getUpcomingRenewals(principal.requireTeamId());
  }
}
