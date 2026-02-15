package com.buurman.controller.backoffice;

import com.buurman.dto.response.backoffice.BackofficeDashboardResponse;
import com.buurman.service.backoffice.BackofficeDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backoffice/dashboard")
@Tag(name = "Backoffice - Dashboard", description = "Platform-wide dashboard statistics")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeDashboardController {

    private final BackofficeDashboardService backofficeDashboardService;


    @Operation(summary = "Get dashboard stats", description = "Get aggregated platform statistics")
    @GetMapping("/stats")
    public BackofficeDashboardResponse getStats() {
        return backofficeDashboardService.getStats();
    }
}
