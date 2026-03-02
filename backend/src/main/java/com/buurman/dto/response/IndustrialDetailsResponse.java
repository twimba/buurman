package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

public record IndustrialDetailsResponse(
    Optional<BigDecimal> clearHeightValue,
    Optional<String> clearHeightUnit,
    Optional<Integer> loadingDocks,
    Optional<Integer> driveInDoors,
    Optional<BigDecimal> floorLoadCapacityValue,
    Optional<String> floorLoadCapacityUnit,
    Optional<Integer> powerCapacityKva,
    Optional<Boolean> hasThreePhasePower,
    Optional<Boolean> hasCrane,
    Optional<BigDecimal> craneCapacityValue,
    Optional<String> craneCapacityUnit,
    Optional<Boolean> hasHazmatCertification,
    Optional<Boolean> hasVentilationSystem,
    Optional<Boolean> hasClimateControl,
    Optional<BigDecimal> yardAreaValue,
    Optional<String> yardAreaUnit,
    Optional<String> zoningClassification) {}
