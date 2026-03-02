package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.api.DashboardApi;
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
  public List<RecentActivityResponse> getRecentActivities(Integer limit) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dashboardService.getRecentActivities(principal.requireTeamId(), limit);
  }
}
