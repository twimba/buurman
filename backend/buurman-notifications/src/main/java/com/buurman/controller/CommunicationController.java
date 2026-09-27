package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Sid;
import com.buurman.dto.response.CommunicationResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.generated.api.CommunicationsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.CommunicationService;

import lombok.RequiredArgsConstructor;

/**
 * Lives in buurman-notifications, not alongside PaymentController: that controller is in
 * buurman-core, which cannot depend on this module. The Communications OpenAPI tag gives these
 * operations their own generated interface so the nested paths work without that dependency.
 */
@RestController
@RequiredArgsConstructor
public class CommunicationController implements CommunicationsApi {

  private final CommunicationService communicationService;

  @Override
  public List<CommunicationResponse> getPaymentCommunications(String identifier) {
    return communicationService.getPaymentCommunications(Sid.of(identifier), currentTeamId());
  }

  @Override
  public List<CommunicationResponse> getContractCommunications(String identifier) {
    return communicationService.getContractCommunications(Sid.of(identifier), currentTeamId());
  }

  private java.util.UUID currentTeamId() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return principal.getTeamId().orElseThrow(() -> new NotFoundException("No team membership"));
  }
}
