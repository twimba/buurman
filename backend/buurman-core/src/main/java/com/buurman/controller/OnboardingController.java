package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CompleteOnboardingRequest;
import com.buurman.dto.response.OnboardingStatusResponse;
import com.buurman.generated.api.OnboardingApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.OnboardingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OnboardingController implements OnboardingApi {

  private final OnboardingService onboardingService;

  @Override
  public OnboardingStatusResponse getOnboardingStatus() {
    return onboardingService.getOnboardingStatus(
        SecurityUtils.getCurrentPrincipal().requireTeamId());
  }

  @Override
  public OnboardingStatusResponse completeOnboarding(
      CompleteOnboardingRequest completeOnboardingRequest) {
    return onboardingService.completeOnboarding(
        completeOnboardingRequest, SecurityUtils.getCurrentPrincipal().requireTeamId());
  }
}
