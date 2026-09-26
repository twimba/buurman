package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.Min;

public record UpdateUnitResidentialDetailsRequest(
    Optional<@Min(value = 0, message = "Bedrooms must be at least 0") Integer> bedrooms,
    Optional<@Min(value = 0, message = "Bathrooms must be at least 0") Integer> bathrooms,
    boolean furnished,
    Optional<String> petPolicy) {}
