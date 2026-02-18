package com.buurman.dto.response;

import java.math.BigDecimal;

public record IndustrialDetailsResponse(
    BigDecimal clearHeightM,
    Integer loadingDocks,
    Integer driveInDoors,
    BigDecimal floorLoadCapacityKgSqm,
    Integer powerCapacityKva,
    Boolean hasThreePhasePower,
    Boolean hasCrane,
    BigDecimal craneCapacityTons,
    Boolean hasHazmatCertification,
    Boolean hasVentilationSystem,
    Boolean hasClimateControl,
    BigDecimal yardAreaValue,
    String yardAreaUnit,
    String zoningClassification) {}
