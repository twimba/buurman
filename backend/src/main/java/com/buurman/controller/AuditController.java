package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.generated.api.AuditLogsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuditController implements AuditLogsApi {

  private final AuditService auditService;

  @SuppressWarnings("unchecked")
  @Override
  public PageResponse getAllAuditLogs(
      String entityType,
      String action,
      String search,
      Integer page,
      Integer size,
      String sort,
      String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    SortDirection sortDirection = SortDirection.valueOf(direction);
    PageRequest pageRequest = PageRequest.of(page, size, sort, sortDirection);
    return auditService.getAllAuditLogsPaginated(
        principal.requireTeamId(), entityType, action, search, pageRequest);
  }
}
