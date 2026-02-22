package com.buurman.dto.response;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record IndustrialDetailsResponse(
    @Nullable BigDecimal clearHeightM,
    @Nullable Integer loadingDocks,
    @Nullable Integer driveInDoors,
    @Nullable BigDecimal floorLoadCapacityKgSqm,
    @Nullable Integer powerCapacityKva,
    @Nullable Boolean hasThreePhasePower,
    @Nullable Boolean hasCrane,
    @Nullable BigDecimal craneCapacityTons,
    @Nullable Boolean hasHazmatCertification,
    @Nullable Boolean hasVentilationSystem,
    @Nullable Boolean hasClimateControl,
    @Nullable BigDecimal yardAreaValue,
    @Nullable String yardAreaUnit,
    @Nullable String zoningClassification) {}
