package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;

@SkipTestCoverage
public record CompleteOnboardingRequest(
    @NotBlank(message = "Country code is required") String countryCode,
    @NotBlank(message = "Currency is required") String currency,
    Optional<String> dateFormat) {}
