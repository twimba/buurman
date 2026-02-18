package com.buurman.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

public record AgriculturalDetailsRequest(
    @Positive(message = "Total land area must be positive") BigDecimal totalLandAreaValue,
    String totalLandAreaUnit,
    @Positive(message = "Arable area must be positive") BigDecimal arableAreaValue,
    String arableAreaUnit,
    String soilType,
    Boolean hasWaterRights,
    String waterSource,
    String irrigationType,
    String fencingType,
    Boolean hasOutbuildings,
    String outbuildingDetails,
    String currentUse,
    String zoningClassification) {}
