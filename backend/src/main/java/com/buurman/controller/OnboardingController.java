package com.buurman.controller;

import java.time.Clock;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.TeamPreferences;
import com.buurman.dto.request.CompleteOnboardingRequest;
import com.buurman.dto.response.OnboardingStatusResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

  private final TeamPreferencesRepository teamPreferencesRepository;
  private final Clock clock;

  @GetMapping("/status")
  public OnboardingStatusResponse getOnboardingStatus() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(principal.requireTeamId());
    return new OnboardingStatusResponse(
        prefs.getOnboardingCompletedAt().isPresent(),
        prefs.getOnboardingCompletedAt(),
        prefs.getDefaultCurrency(),
        prefs.getDefaultCountryCode());
  }

  @PostMapping("/complete")
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ResponseEntity<OnboardingStatusResponse> completeOnboarding(
      @Valid @RequestBody CompleteOnboardingRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(principal.requireTeamId());

    if (prefs.getOnboardingCompletedAt().isPresent()) {
      throw new BadRequestException("Onboarding has already been completed");
    }

    // Validate ISO 4217 currency code
    try {
      java.util.Currency.getInstance(request.currency());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid ISO 4217 currency code: " + request.currency());
    }

    prefs.setDefaultCountryCode(request.countryCode());
    prefs.setDefaultCurrency(request.currency().toUpperCase());
    request.dateFormat().ifPresent(prefs::setDateFormat);
    prefs.setOnboardingCompletedAt(Optional.of(clock.instant()));
    teamPreferencesRepository.save(prefs);

    return ResponseEntity.ok(
        new OnboardingStatusResponse(
            true,
            prefs.getOnboardingCompletedAt(),
            prefs.getDefaultCurrency(),
            prefs.getDefaultCountryCode()));
  }
}
