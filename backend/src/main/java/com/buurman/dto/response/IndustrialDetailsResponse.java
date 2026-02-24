package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

public record IndustrialDetailsResponse(
    Optional<BigDecimal> clearHeightM,
    Optional<Integer> loadingDocks,
    Optional<Integer> driveInDoors,
    Optional<BigDecimal> floorLoadCapacityKgSqm,
    Optional<Integer> powerCapacityKva,
    Optional<Boolean> hasThreePhasePower,
    Optional<Boolean> hasCrane,
    Optional<BigDecimal> craneCapacityTons,
    Optional<Boolean> hasHazmatCertification,
    Optional<Boolean> hasVentilationSystem,
    Optional<Boolean> hasClimateControl,
    Optional<BigDecimal> yardAreaValue,
    Optional<String> yardAreaUnit,
    Optional<String> zoningClassification) {}
