package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record IndustrialDetailsRequest(
    @Positive(message = "Clear height must be positive") Optional<BigDecimal> clearHeightM,
    @Min(value = 0, message = "Loading docks must be non-negative") Optional<Integer> loadingDocks,
    @Min(value = 0, message = "Drive-in doors must be non-negative") Optional<Integer> driveInDoors,
    @Positive(message = "Floor load capacity must be positive") Optional<BigDecimal> floorLoadCapacityKgSqm,
    @Positive(message = "Power capacity must be positive") Optional<Integer> powerCapacityKva,
    Optional<Boolean> hasThreePhasePower,
    Optional<Boolean> hasCrane,
    @Positive(message = "Crane capacity must be positive") Optional<BigDecimal> craneCapacityTons,
    Optional<Boolean> hasHazmatCertification,
    Optional<Boolean> hasVentilationSystem,
    Optional<Boolean> hasClimateControl,
    @Positive(message = "Yard area must be positive") Optional<BigDecimal> yardAreaValue,
    Optional<String> yardAreaUnit,
    Optional<String> zoningClassification) {

  public IndustrialDetailsRequest {
    clearHeightM = Objects.requireNonNullElse(clearHeightM, Optional.empty());
    loadingDocks = Objects.requireNonNullElse(loadingDocks, Optional.empty());
    driveInDoors = Objects.requireNonNullElse(driveInDoors, Optional.empty());
    floorLoadCapacityKgSqm = Objects.requireNonNullElse(floorLoadCapacityKgSqm, Optional.empty());
    powerCapacityKva = Objects.requireNonNullElse(powerCapacityKva, Optional.empty());
    hasThreePhasePower = Objects.requireNonNullElse(hasThreePhasePower, Optional.empty());
    hasCrane = Objects.requireNonNullElse(hasCrane, Optional.empty());
    craneCapacityTons = Objects.requireNonNullElse(craneCapacityTons, Optional.empty());
    hasHazmatCertification = Objects.requireNonNullElse(hasHazmatCertification, Optional.empty());
    hasVentilationSystem = Objects.requireNonNullElse(hasVentilationSystem, Optional.empty());
    hasClimateControl = Objects.requireNonNullElse(hasClimateControl, Optional.empty());
    yardAreaValue = Objects.requireNonNullElse(yardAreaValue, Optional.empty());
    yardAreaUnit = Objects.requireNonNullElse(yardAreaUnit, Optional.empty());
    zoningClassification = Objects.requireNonNullElse(zoningClassification, Optional.empty());
  }
}
