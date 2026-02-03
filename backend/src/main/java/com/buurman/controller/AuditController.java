package com.buurman.controller;

import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Activity log management")
@SecurityRequirement(name = "bearer-jwt")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Get all audit logs", description = "Get all activity logs for the team with optional filtering")
    @GetMapping
    public List<RecentActivityResponse> getAllAuditLogs(
            @Parameter(description = "Filter by entity type (PROPERTY, TENANT, CONTRACT, PAYMENT, EXPENSE)")
            @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by action (CREATE, UPDATE, DELETE, RESTORE)")
            @RequestParam(required = false) String action,
            @Parameter(description = "Search by user name, entity type, or action")
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal) {
        return auditService.getAllAuditLogs(
                principal.getTeamId(),
                entityType,
                action,
                search
        );
    }
}
