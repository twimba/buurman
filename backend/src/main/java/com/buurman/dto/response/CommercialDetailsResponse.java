package com.buurman.dto.response;

import java.math.BigDecimal;

public record CommercialDetailsResponse(
    BigDecimal usableAreaValue,
    String usableAreaUnit,
    BigDecimal commonAreaValue,
    String commonAreaUnit,
    Integer floorLevel,
    BigDecimal ceilingHeightM,
    Boolean hasStorefront,
    Boolean hasSignageRights,
    String zoningClassification,
    Integer maxOccupancy,
    Integer restroomCount,
    Boolean hasKitchenFacility,
    Boolean accessibilityCompliant) {}
