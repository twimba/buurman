package com.buurman.service;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.TeamPreferences;
import com.buurman.dto.request.CompleteOnboardingRequest;
import com.buurman.dto.response.OnboardingStatusResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.TeamPreferencesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {

  private final TeamPreferencesRepository teamPreferencesRepository;
  private final Clock clock;

  public OnboardingStatusResponse getOnboardingStatus(UUID teamId) {
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(teamId);
    return new OnboardingStatusResponse(
        prefs.getOnboardingCompletedAt().isPresent(),
        prefs.getOnboardingCompletedAt(),
        prefs.getDefaultCurrency(),
        prefs.getDefaultCountryCode());
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public OnboardingStatusResponse completeOnboarding(
      CompleteOnboardingRequest request, UUID teamId) {
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(teamId);

    if (prefs.getOnboardingCompletedAt().isPresent()) {
      throw new BadRequestException("Onboarding has already been completed");
    }

    try {
      java.util.Currency.getInstance(request.currency());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Invalid ISO 4217 currency code: " + request.currency());
    }

    prefs.setDefaultCountryCode(request.countryCode());
    prefs.setDefaultCurrency(request.currency().toUpperCase(Locale.ROOT));
    request.dateFormat().ifPresent(prefs::setDateFormat);
    prefs.setOnboardingCompletedAt(Optional.of(clock.instant()));
    teamPreferencesRepository.save(prefs);

    return new OnboardingStatusResponse(
        true,
        prefs.getOnboardingCompletedAt(),
        prefs.getDefaultCurrency(),
        prefs.getDefaultCountryCode());
  }
}
