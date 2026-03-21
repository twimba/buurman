package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;

@Generated
public record CompleteOnboardingRequest(
    @NotBlank(message = "Country code is required") String countryCode,
    @NotBlank(message = "Currency is required") String currency,
    Optional<String> dateFormat) {}
