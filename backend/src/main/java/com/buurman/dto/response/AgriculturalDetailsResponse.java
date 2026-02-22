package com.buurman.dto.response;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record AgriculturalDetailsResponse(
    @Nullable BigDecimal totalLandAreaValue,
    @Nullable String totalLandAreaUnit,
    @Nullable BigDecimal arableAreaValue,
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
