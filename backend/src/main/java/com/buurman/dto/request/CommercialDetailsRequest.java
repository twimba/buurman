package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record CommercialDetailsRequest(
    Optional<@Positive(message = "Usable area must be positive") BigDecimal> usableAreaValue,
    Optional<String> usableAreaUnit,
    Optional<@Positive(message = "Common area must be positive") BigDecimal> commonAreaValue,
    Optional<String> commonAreaUnit,
    Optional<Integer> floorLevel,
    Optional<@Positive(message = "Ceiling height must be positive") BigDecimal> ceilingHeightM,
    Optional<Boolean> hasStorefront,
    Optional<Boolean> hasSignageRights,
    Optional<String> zoningClassification,
    Optional<@Positive(message = "Max occupancy must be positive") Integer> maxOccupancy,
    Optional<@Min(value = 0, message = "Restroom count must be non-negative") Integer>
        restroomCount,
    Optional<Boolean> hasKitchenFacility,
    Optional<Boolean> accessibilityCompliant) {}
