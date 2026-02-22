package com.buurman.dto.request;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Positive;

public record AgriculturalDetailsRequest(
    @Nullable @Positive(message = "Total land area must be positive") BigDecimal totalLandAreaValue,
    @Nullable String totalLandAreaUnit,
    @Nullable @Positive(message = "Arable area must be positive") BigDecimal arableAreaValue,
    @Nullable String arableAreaUnit,
    @Nullable String soilType,
    @Nullable Boolean hasWaterRights,
    @Nullable String waterSource,
    @Nullable String irrigationType,
    @Nullable String fencingType,
    @Nullable Boolean hasOutbuildings,
    @Nullable String outbuildingDetails,
    @Nullable String currentUse,
    @Nullable String zoningClassification) {}
