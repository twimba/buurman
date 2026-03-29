package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Positive;

@SkipTestCoverage
public record AgriculturalDetailsRequest(
    Optional<@Positive(message = "Total land area must be positive") BigDecimal> totalLandAreaValue,
    Optional<String> totalLandAreaUnit,
    Optional<@Positive(message = "Arable area must be positive") BigDecimal> arableAreaValue,
    Optional<String> arableAreaUnit,
    Optional<String> soilType,
    Optional<Boolean> hasWaterRights,
    Optional<String> waterSource,
    Optional<String> irrigationType,
    Optional<String> fencingType,
    Optional<Boolean> hasOutbuildings,
    Optional<String> outbuildingDetails,
    Optional<String> currentUse,
    Optional<String> zoningClassification) {}
