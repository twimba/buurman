package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Objects;
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
    Optional<String> zoningClassification) {

  public AgriculturalDetailsRequest {
    totalLandAreaValue = Objects.requireNonNullElse(totalLandAreaValue, Optional.empty());
    totalLandAreaUnit = Objects.requireNonNullElse(totalLandAreaUnit, Optional.empty());
    arableAreaValue = Objects.requireNonNullElse(arableAreaValue, Optional.empty());
    arableAreaUnit = Objects.requireNonNullElse(arableAreaUnit, Optional.empty());
    soilType = Objects.requireNonNullElse(soilType, Optional.empty());
    hasWaterRights = Objects.requireNonNullElse(hasWaterRights, Optional.empty());
    waterSource = Objects.requireNonNullElse(waterSource, Optional.empty());
    irrigationType = Objects.requireNonNullElse(irrigationType, Optional.empty());
    fencingType = Objects.requireNonNullElse(fencingType, Optional.empty());
    hasOutbuildings = Objects.requireNonNullElse(hasOutbuildings, Optional.empty());
    outbuildingDetails = Objects.requireNonNullElse(outbuildingDetails, Optional.empty());
    currentUse = Objects.requireNonNullElse(currentUse, Optional.empty());
    zoningClassification = Objects.requireNonNullElse(zoningClassification, Optional.empty());
  }
}
