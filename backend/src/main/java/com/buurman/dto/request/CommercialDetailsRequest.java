package com.buurman.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record CommercialDetailsRequest(
    @Positive(message = "Usable area must be positive") BigDecimal usableAreaValue,
    String usableAreaUnit,
    @Positive(message = "Common area must be positive") BigDecimal commonAreaValue,
    String commonAreaUnit,
    Integer floorLevel,
    @Positive(message = "Ceiling height must be positive") BigDecimal ceilingHeightM,
    Boolean hasStorefront,
    Boolean hasSignageRights,
    String zoningClassification,
    @Positive(message = "Max occupancy must be positive") Integer maxOccupancy,
    @Min(value = 0, message = "Restroom count must be non-negative") Integer restroomCount,
    Boolean hasKitchenFacility,
    Boolean accessibilityCompliant) {}
