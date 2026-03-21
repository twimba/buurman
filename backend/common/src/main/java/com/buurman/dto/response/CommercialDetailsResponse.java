package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;
import com.buurman.util.Generated;

@Generated
public record CommercialDetailsResponse(
    Optional<BigDecimal> usableAreaValue,
    Optional<String> usableAreaUnit,
    Optional<BigDecimal> commonAreaValue,
    Optional<String> commonAreaUnit,
    Optional<Integer> floorLevel,
    Optional<BigDecimal> ceilingHeightValue,
    Optional<String> ceilingHeightUnit,
    Optional<Boolean> hasStorefront,
    Optional<Boolean> hasSignageRights,
    Optional<String> zoningClassification,
    Optional<Integer> maxOccupancy,
    Optional<Integer> restroomCount,
    Optional<Boolean> hasKitchenFacility,
    Optional<Boolean> accessibilityCompliant) {}
