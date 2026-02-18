package com.buurman.dto.response;

import java.math.BigDecimal;

public record AgriculturalDetailsResponse(
    BigDecimal totalLandAreaValue,
    String totalLandAreaUnit,
    BigDecimal arableAreaValue,
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
