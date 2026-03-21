package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record AgriculturalDetailsResponse(
    Optional<BigDecimal> totalLandAreaValue,
    Optional<String> totalLandAreaUnit,
    Optional<BigDecimal> arableAreaValue,
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
