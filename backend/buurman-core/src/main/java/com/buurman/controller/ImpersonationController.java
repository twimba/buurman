package com.buurman.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.buurman.domain.ImpersonationEndReason;
import com.buurman.dto.request.ExchangeTokenRequest;
import com.buurman.dto.response.ImpersonationExchangeResponse;
import com.buurman.dto.response.ImpersonationSessionInfo;
import com.buurman.generated.api.ImpersonationApi;
import com.buurman.security.ImpersonationPrincipal;
import com.buurman.service.ImpersonationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ImpersonationController implements ImpersonationApi {

  private final ImpersonationService impersonationService;

  @Override
  public ImpersonationExchangeResponse exchangeImpersonationToken(ExchangeTokenRequest request) {
    return impersonationService.exchangeToken(request);
  }

  @Override
  public ImpersonationSessionInfo getImpersonationSessionInfo() {
    ImpersonationPrincipal principal = requireImpersonationPrincipal();
    return impersonationService.getSessionInfo(principal.getImpersonationSessionId());
  }

  @Override
  public void endImpersonationSession() {
    ImpersonationPrincipal principal = requireImpersonationPrincipal();
    impersonationService.endSession(
        principal.getImpersonationSessionId(), ImpersonationEndReason.MANUAL);
  }

  private static ImpersonationPrincipal requireImpersonationPrincipal() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof ImpersonationPrincipal impersonationPrincipal) {
      return impersonationPrincipal;
    }
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "This endpoint requires an impersonation session");
  }
}
