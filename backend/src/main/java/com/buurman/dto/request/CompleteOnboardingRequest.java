package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

public record CompleteOnboardingRequest(
    @NotBlank(message = "Country is required") String country,
    @NotBlank(message = "Currency is required") String currency,
    Optional<String> dateFormat) {}
