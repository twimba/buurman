package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

public record OnboardingStatusResponse(
    boolean completed,
    Optional<Instant> completedAt,
    String currentCurrency,
    String currentCountryCode) {}
