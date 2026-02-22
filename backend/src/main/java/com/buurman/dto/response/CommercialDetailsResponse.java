package com.buurman.dto.response;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record CommercialDetailsResponse(
    @Nullable BigDecimal usableAreaValue,
    @Nullable String usableAreaUnit,
    @Nullable BigDecimal commonAreaValue,
    @Nullable String commonAreaUnit,
    @Nullable Integer floorLevel,
    @Nullable BigDecimal ceilingHeightM,
    @Nullable Boolean hasStorefront,
    @Nullable Boolean hasSignageRights,
    @Nullable String zoningClassification,
    @Nullable Integer maxOccupancy,
    @Nullable Integer restroomCount,
    @Nullable Boolean hasKitchenFacility,
    @Nullable Boolean accessibilityCompliant) {}
