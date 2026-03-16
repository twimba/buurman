package com.buurman.controller.backoffice;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.domain.identifier.ImpersonationSessionIdentifier;
import com.buurman.dto.request.CreateImpersonationRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.RejoinImpersonationRequest;
import com.buurman.dto.response.CreateImpersonationResponse;
import com.buurman.dto.response.ImpersonationSessionResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.generated.backoffice.api.BackofficeImpersonationApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.ImpersonationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeImpersonationController implements BackofficeImpersonationApi {

  private final ImpersonationService impersonationService;
  private final HttpServletRequest httpServletRequest;

  @Override
  public CreateImpersonationResponse createImpersonationSession(
      CreateImpersonationRequest request) {
    String ipAddress = extractIpAddress(httpServletRequest);
    return impersonationService.createSession(
        request, SecurityUtils.getBackofficePrincipal(), ipAddress);
  }

  @Override
  public PageResponse listImpersonationSessions(
      Optional<String> adminEmail,
      Optional<String> targetUserEmail,
      Optional<String> teamIdentifier,
      Optional<String> status,
      Optional<String> mode,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    SortDirection sortDirection = direction.map(SortDirection::valueOf).orElse(SortDirection.DESC);
    return impersonationService.listSessions(
        PageRequest.of(page.orElse(null), size.orElse(null), sort.orElse(null), sortDirection),
        adminEmail.orElse(null),
        targetUserEmail.orElse(null),
        teamIdentifier.orElse(null),
        status.orElse(null),
        mode.orElse(null));
  }

  @Override
  public ImpersonationSessionResponse getImpersonationSession(
      ImpersonationSessionIdentifier identifier) {
    return impersonationService.getSessionDetail(identifier);
  }

  @Override
  public CreateImpersonationResponse rejoinImpersonationSession(
      ImpersonationSessionIdentifier identifier, RejoinImpersonationRequest request) {
    return impersonationService.rejoinSession(
        identifier, SecurityUtils.getBackofficePrincipal(), request.password());
  }

  @Override
  public void terminateImpersonationSession(ImpersonationSessionIdentifier identifier) {
    impersonationService.terminateSession(identifier);
  }

  private static String extractIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",", 2)[0].trim();
    }
    return request.getRemoteAddr();
  }
}
