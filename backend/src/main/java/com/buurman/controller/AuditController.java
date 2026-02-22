package com.buurman.controller;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.AuditEntityType;
import com.buurman.domain.AuditLog.Action;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/audit-logs")
@Tag(name = "Audit Logs", description = "Activity log management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class AuditController {

  private final AuditService auditService;

  @Operation(
      summary = "Get all audit logs",
      description = "Get all activity logs for the team with optional filtering and pagination")
  @GetMapping
  public PageResponse<RecentActivityResponse> getAllAuditLogs(
      @Parameter(
              description = "Filter by entity type (PROPERTY, TENANT, CONTRACT, PAYMENT, EXPENSE)")
          @RequestParam(required = false)
          @Nullable AuditEntityType entityType,
      @Parameter(description = "Filter by action (CREATE, UPDATE, DELETE, RESTORE)")
          @RequestParam(required = false)
          @Nullable Action action,
      @Parameter(description = "Search by user name, entity type, or action")
          @RequestParam(required = false)
          @Nullable String search,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam(required = false) @Nullable String sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return auditService.getAllAuditLogsPaginated(
        principal.getTeamId(),
        entityType != null ? entityType.name() : null,
        action != null ? action.name() : null,
        search,
        pageRequest);
  }
}
