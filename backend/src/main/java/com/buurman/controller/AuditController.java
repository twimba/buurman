package com.buurman.controller;

import java.util.Optional;

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
          @RequestParam
          Optional<AuditEntityType> entityType,
      @Parameter(description = "Filter by action (CREATE, UPDATE, DELETE, RESTORE)") @RequestParam
          Optional<Action> action,
      @Parameter(description = "Search by user name, entity type, or action") @RequestParam
          Optional<String> search,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam Optional<String> sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {
    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return auditService.getAllAuditLogsPaginated(
        principal.requireTeamId(),
        entityType.map(AuditEntityType::name).orElse(null),
        action.map(Action::name).orElse(null),
        search.orElse(null),
        pageRequest);
  }
}
