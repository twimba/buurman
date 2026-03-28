package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Min;

@SkipTestCoverage
public record ResidentialDetailsRequest(
    Optional<@Min(value = 0, message = "Bedrooms must be non-negative") Integer> bedrooms,
    Optional<@Min(value = 0, message = "Bathrooms must be non-negative") Integer> bathrooms,
    Optional<Boolean> furnished,
    Optional<String> petPolicy) {}
