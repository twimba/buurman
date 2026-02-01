package com.buurman.controller;

import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and recent activities")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get dashboard statistics", description = "Get overall statistics for the dashboard")
    @GetMapping("/stats")
    public DashboardStatsResponse getDashboardStats(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getDashboardStats(principal.getTeamId());
    }

    @Operation(summary = "Get recent activities", description = "Get recent audit trail activities")
    @GetMapping("/recent-activities")
    public List<RecentActivityResponse> getRecentActivities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecentActivities(principal.getTeamId(), limit);
    }
}
