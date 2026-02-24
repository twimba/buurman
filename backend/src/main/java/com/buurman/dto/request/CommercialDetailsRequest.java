package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record CommercialDetailsRequest(
    @Positive(message = "Usable area must be positive") Optional<BigDecimal> usableAreaValue,
    Optional<String> usableAreaUnit,
    @Positive(message = "Common area must be positive") Optional<BigDecimal> commonAreaValue,
    Optional<String> commonAreaUnit,
    Optional<Integer> floorLevel,
    @Positive(message = "Ceiling height must be positive") Optional<BigDecimal> ceilingHeightM,
    Optional<Boolean> hasStorefront,
    Optional<Boolean> hasSignageRights,
    Optional<String> zoningClassification,
    @Positive(message = "Max occupancy must be positive") Optional<Integer> maxOccupancy,
    @Min(value = 0, message = "Restroom count must be non-negative") Optional<Integer> restroomCount,
    Optional<Boolean> hasKitchenFacility,
    Optional<Boolean> accessibilityCompliant) {

  public CommercialDetailsRequest {
    usableAreaValue = Objects.requireNonNullElse(usableAreaValue, Optional.empty());
    usableAreaUnit = Objects.requireNonNullElse(usableAreaUnit, Optional.empty());
    commonAreaValue = Objects.requireNonNullElse(commonAreaValue, Optional.empty());
    commonAreaUnit = Objects.requireNonNullElse(commonAreaUnit, Optional.empty());
    floorLevel = Objects.requireNonNullElse(floorLevel, Optional.empty());
    ceilingHeightM = Objects.requireNonNullElse(ceilingHeightM, Optional.empty());
    hasStorefront = Objects.requireNonNullElse(hasStorefront, Optional.empty());
    hasSignageRights = Objects.requireNonNullElse(hasSignageRights, Optional.empty());
    zoningClassification = Objects.requireNonNullElse(zoningClassification, Optional.empty());
    maxOccupancy = Objects.requireNonNullElse(maxOccupancy, Optional.empty());
    restroomCount = Objects.requireNonNullElse(restroomCount, Optional.empty());
    hasKitchenFacility = Objects.requireNonNullElse(hasKitchenFacility, Optional.empty());
    accessibilityCompliant = Objects.requireNonNullElse(accessibilityCompliant, Optional.empty());
  }
}
