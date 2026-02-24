package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.Min;

public record ResidentialDetailsRequest(
    @Min(value = 0, message = "Bedrooms must be non-negative") Optional<Integer> bedrooms,
    @Min(value = 0, message = "Bathrooms must be non-negative") Optional<Integer> bathrooms,
    Optional<Boolean> furnished,
    Optional<String> petPolicy) {}
