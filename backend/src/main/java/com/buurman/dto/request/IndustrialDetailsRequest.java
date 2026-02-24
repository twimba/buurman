package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record IndustrialDetailsRequest(
    Optional<@Positive(message = "Clear height must be positive") BigDecimal> clearHeightM,
    Optional<@Min(value = 0, message = "Loading docks must be non-negative") Integer> loadingDocks,
    Optional<@Min(value = 0, message = "Drive-in doors must be non-negative") Integer> driveInDoors,
    Optional<@Positive(message = "Floor load capacity must be positive") BigDecimal>
        floorLoadCapacityKgSqm,
    Optional<@Positive(message = "Power capacity must be positive") Integer> powerCapacityKva,
    Optional<Boolean> hasThreePhasePower,
    Optional<Boolean> hasCrane,
    Optional<@Positive(message = "Crane capacity must be positive") BigDecimal> craneCapacityTons,
    Optional<Boolean> hasHazmatCertification,
    Optional<Boolean> hasVentilationSystem,
    Optional<Boolean> hasClimateControl,
    Optional<@Positive(message = "Yard area must be positive") BigDecimal> yardAreaValue,
    Optional<String> yardAreaUnit,
    Optional<String> zoningClassification) {}
