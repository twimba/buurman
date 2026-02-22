package com.buurman.dto.request;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record IndustrialDetailsRequest(
    @Nullable @Positive(message = "Clear height must be positive") BigDecimal clearHeightM,
    @Nullable @Min(value = 0, message = "Loading docks must be non-negative") Integer loadingDocks,
    @Nullable @Min(value = 0, message = "Drive-in doors must be non-negative") Integer driveInDoors,
    @Nullable @Positive(message = "Floor load capacity must be positive") BigDecimal floorLoadCapacityKgSqm,
    @Nullable @Positive(message = "Power capacity must be positive") Integer powerCapacityKva,
    @Nullable Boolean hasThreePhasePower,
    @Nullable Boolean hasCrane,
    @Nullable @Positive(message = "Crane capacity must be positive") BigDecimal craneCapacityTons,
    @Nullable Boolean hasHazmatCertification,
    @Nullable Boolean hasVentilationSystem,
    @Nullable Boolean hasClimateControl,
    @Nullable @Positive(message = "Yard area must be positive") BigDecimal yardAreaValue,
    @Nullable String yardAreaUnit,
    @Nullable String zoningClassification) {}
