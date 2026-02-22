package com.buurman.dto.request;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record CommercialDetailsRequest(
    @Nullable @Positive(message = "Usable area must be positive") BigDecimal usableAreaValue,
    @Nullable String usableAreaUnit,
    @Nullable @Positive(message = "Common area must be positive") BigDecimal commonAreaValue,
    @Nullable String commonAreaUnit,
    @Nullable Integer floorLevel,
    @Nullable @Positive(message = "Ceiling height must be positive") BigDecimal ceilingHeightM,
    @Nullable Boolean hasStorefront,
    @Nullable Boolean hasSignageRights,
    @Nullable String zoningClassification,
    @Nullable @Positive(message = "Max occupancy must be positive") Integer maxOccupancy,
    @Nullable @Min(value = 0, message = "Restroom count must be non-negative") Integer restroomCount,
    @Nullable Boolean hasKitchenFacility,
    @Nullable Boolean accessibilityCompliant) {}
