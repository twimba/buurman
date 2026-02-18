package com.buurman.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record IndustrialDetailsRequest(
    @Positive(message = "Clear height must be positive") BigDecimal clearHeightM,
    @Min(value = 0, message = "Loading docks must be non-negative") Integer loadingDocks,
    @Min(value = 0, message = "Drive-in doors must be non-negative") Integer driveInDoors,
    @Positive(message = "Floor load capacity must be positive") BigDecimal floorLoadCapacityKgSqm,
    @Positive(message = "Power capacity must be positive") Integer powerCapacityKva,
    Boolean hasThreePhasePower,
    Boolean hasCrane,
    @Positive(message = "Crane capacity must be positive") BigDecimal craneCapacityTons,
    Boolean hasHazmatCertification,
    Boolean hasVentilationSystem,
    Boolean hasClimateControl,
    @Positive(message = "Yard area must be positive") BigDecimal yardAreaValue,
    String yardAreaUnit,
    String zoningClassification) {}
