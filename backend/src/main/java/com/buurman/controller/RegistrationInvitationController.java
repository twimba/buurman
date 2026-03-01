package com.buurman.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.RegistrationConfigResponse;
import com.buurman.dto.response.ValidateInvitationCodeResponse;
import com.buurman.generated.api.RegistrationApi;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.RegistrationInvitationService;
import com.buurman.util.FeatureFlags;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RegistrationInvitationController implements RegistrationApi {

  private final RegistrationInvitationService invitationService;
  private final FeatureFlagService featureFlagService;

  @Override
  public ValidateInvitationCodeResponse validate(Map<String, String> requestBody) {
    String code = requestBody.getOrDefault("code", "");
    return invitationService.validateCode(code);
  }

  @Override
  public RegistrationConfigResponse getConfig() {
    boolean invitationRequired = featureFlagService.isEnabled(FeatureFlags.INVITATION_REQUIRED);
    return new RegistrationConfigResponse(invitationRequired);
  }
}
