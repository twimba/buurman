package com.buurman.controller;

import java.time.Clock;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.TeamPreferences;
import com.buurman.dto.request.CompleteOnboardingRequest;
import com.buurman.dto.response.OnboardingStatusResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.generated.api.OnboardingApi;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OnboardingController implements OnboardingApi {

  private final TeamPreferencesRepository teamPreferencesRepository;
  private final Clock clock;

  @Override
  public OnboardingStatusResponse getOnboardingStatus() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(principal.requireTeamId());
    return new OnboardingStatusResponse(
        prefs.getOnboardingCompletedAt().isPresent(),
        prefs.getOnboardingCompletedAt(),
        prefs.getDefaultCurrency(),
        prefs.getDefaultCountryCode());
  }

  @Override
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public OnboardingStatusResponse completeOnboarding(
      CompleteOnboardingRequest completeOnboardingRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(principal.requireTeamId());

    if (prefs.getOnboardingCompletedAt().isPresent()) {
      throw new BadRequestException("Onboarding has already been completed");
    }

    try {
      java.util.Currency.getInstance(completeOnboardingRequest.currency());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Invalid ISO 4217 currency code: " + completeOnboardingRequest.currency());
    }

    prefs.setDefaultCountryCode(completeOnboardingRequest.countryCode());
    prefs.setDefaultCurrency(completeOnboardingRequest.currency().toUpperCase());
    completeOnboardingRequest.dateFormat().ifPresent(prefs::setDateFormat);
    prefs.setOnboardingCompletedAt(Optional.of(clock.instant()));
    teamPreferencesRepository.save(prefs);

    return new OnboardingStatusResponse(
        true,
        prefs.getOnboardingCompletedAt(),
        prefs.getDefaultCurrency(),
        prefs.getDefaultCountryCode());
  }
}
