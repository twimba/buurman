package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.validation.constraints.Positive;

public record AgriculturalDetailsRequest(
    @Positive(message = "Total land area must be positive") Optional<BigDecimal> totalLandAreaValue,
    Optional<String> totalLandAreaUnit,
    @Positive(message = "Arable area must be positive") Optional<BigDecimal> arableAreaValue,
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
