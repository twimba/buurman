package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

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
      Optional<String> entityType,
      Optional<String> action,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return auditService.getAllAuditLogsPaginated(
        principal.requireTeamId(),
        entityType.orElse(null),
        action.orElse(null),
        search.orElse(null),
        pageRequest);
  }
}
