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
    if (contractIdentifier == null) {
      contractIdentifier = Optional.empty();
    }
    if (surfaceAreaSqm == null) {
      surfaceAreaSqm = Optional.empty();
    }
    if (numberOfRooms == null) {
      numberOfRooms = Optional.empty();
    }
    if (numberOfHeatedRooms == null) {
      numberOfHeatedRooms = Optional.empty();
    }
    if (energyLabel == null) {
      energyLabel = Optional.empty();
    }
    if (kitchenQualityPoints == null) {
      kitchenQualityPoints = Optional.empty();
    }
    if (bathroomQualityPoints == null) {
      bathroomQualityPoints = Optional.empty();
    }
    if (wozValue == null) {
      wozValue = Optional.empty();
    }
    if (outdoorSpaceSqm == null) {
      outdoorSpaceSqm = Optional.empty();
    }
    if (parkingType == null) {
      parkingType = Optional.empty();
    }
    if (parkingSpaces == null) {
      parkingSpaces = Optional.empty();
    }
    if (locationBonus == null) {
      locationBonus = Optional.empty();
    }
    if (renovationInvestment == null) {
      renovationInvestment = Optional.empty();
    }
    if (accessibilityFeatures == null) {
      accessibilityFeatures = Optional.empty();
    }
    if (commonAreaSqm == null) {
      commonAreaSqm = Optional.empty();
    }
  }
}
