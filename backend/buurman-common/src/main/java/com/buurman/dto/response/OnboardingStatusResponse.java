package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record OnboardingStatusResponse(
    boolean completed,
    Optional<Instant> completedAt,
    String currentCurrency,
    String currentCountryCode) {}
