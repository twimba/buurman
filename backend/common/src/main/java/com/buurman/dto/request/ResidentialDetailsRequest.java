package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.constraints.Min;

@Generated
public record ResidentialDetailsRequest(
    Optional<@Min(value = 0, message = "Bedrooms must be non-negative") Integer> bedrooms,
    Optional<@Min(value = 0, message = "Bathrooms must be non-negative") Integer> bathrooms,
    Optional<Boolean> furnished,
    Optional<String> petPolicy) {}
