package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.identifier.PropertyIdentifier;

import jakarta.validation.constraints.NotNull;

public record WwsCalculationRequest(
    @NotNull String systemVersion,
    @NotNull PropertyIdentifier propertyIdentifier,
    Optional<String> contractIdentifier,
    Optional<BigDecimal> surfaceAreaSqm,
    Optional<Integer> numberOfRooms,
    Optional<Integer> numberOfHeatedRooms,
    Optional<String> energyLabel,
    Optional<BigDecimal> kitchenQualityPoints,
    Optional<BigDecimal> bathroomQualityPoints,
    Optional<BigDecimal> wozValue,
    Optional<BigDecimal> outdoorSpaceSqm,
    Optional<String> parkingType,
    Optional<Integer> parkingSpaces,
    Optional<BigDecimal> locationBonus,
    Optional<BigDecimal> renovationInvestment,
    Optional<Integer> accessibilityFeatures,
    Optional<BigDecimal> commonAreaSqm) {

  public WwsCalculationRequest {
    if (contractIdentifier.isEmpty()) {
      contractIdentifier = Optional.empty();
    }
    if (surfaceAreaSqm.isEmpty()) {
      surfaceAreaSqm = Optional.empty();
    }
    if (numberOfRooms.isEmpty()) {
      numberOfRooms = Optional.empty();
    }
    if (numberOfHeatedRooms.isEmpty()) {
      numberOfHeatedRooms = Optional.empty();
    }
    if (energyLabel.isEmpty()) {
      energyLabel = Optional.empty();
    }
    if (kitchenQualityPoints.isEmpty()) {
      kitchenQualityPoints = Optional.empty();
    }
    if (bathroomQualityPoints.isEmpty()) {
      bathroomQualityPoints = Optional.empty();
    }
    if (wozValue.isEmpty()) {
      wozValue = Optional.empty();
    }
    if (outdoorSpaceSqm.isEmpty()) {
      outdoorSpaceSqm = Optional.empty();
    }
    if (parkingType.isEmpty()) {
      parkingType = Optional.empty();
    }
    if (parkingSpaces.isEmpty()) {
      parkingSpaces = Optional.empty();
    }
    if (locationBonus.isEmpty()) {
      locationBonus = Optional.empty();
    }
    if (renovationInvestment.isEmpty()) {
      renovationInvestment = Optional.empty();
    }
    if (accessibilityFeatures.isEmpty()) {
      accessibilityFeatures = Optional.empty();
    }
    if (commonAreaSqm.isEmpty()) {
      commonAreaSqm = Optional.empty();
    }
  }
}
