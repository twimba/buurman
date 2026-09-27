package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.Property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyRequest(
    @NotNull(message = "Property type is required") Property.PropertyType propertyType,
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @NotBlank(message = "Postal code is required") String postalCode,
    @NotBlank(message = "Country code is required") String countryCode,
    Optional<String> regionCode,
    Optional<BigDecimal> latitude,
    Optional<BigDecimal> longitude,
    Optional<String> geocodeAccuracy,

    // Construction & Structure
    Optional<Integer> yearBuilt,
    Optional<Integer> yearLastRenovated,
    Optional<String> constructionType,
    Optional<String> foundationType,
    Optional<String> roofType,
    Optional<String> wallConstruction,
    Optional<Integer> numberOfFloors,
    Optional<String> structuralNotes,

    // Utilities & Connections
    Optional<String> electricityConnectionType,
    Optional<Integer> electricityCapacityValue,
    Optional<String> electricityCapacityUnit,
    Optional<String> waterConnectionType,
    Optional<Boolean> hasGasConnection,
    Optional<String> sewageType,
    Optional<String> internetConnectionType,
    Optional<Integer> internetMaxSpeedValue,
    Optional<String> internetMaxSpeedUnit,
    Optional<String> internetStatus,

    // Parking
    Optional<Integer> parkingSpaces,
    Optional<String> parkingType,

    // Safety & Security
    Optional<Boolean> hasSprinklerSystem,
    Optional<Boolean> hasAlarmSystem,
    Optional<Boolean> hasSecurityCameras,
    Optional<Boolean> hasSecureEntry,
    Optional<String> safetyNotes,

    // Accessibility
    Optional<Boolean> isWheelchairAccessible,
    Optional<Boolean> hasElevator,
    Optional<Boolean> hasStepFreeEntrance,

    // Category-specific details (only matching category should be provided)
    // Note: propertyCategory is NOT here — it's immutable after creation
    @Valid Optional<ResidentialDetailsRequest> residentialDetails,
    @Valid Optional<CommercialDetailsRequest> commercialDetails,
    @Valid Optional<IndustrialDetailsRequest> industrialDetails,
    @Valid Optional<AgriculturalDetailsRequest> agriculturalDetails) {

  public UpdatePropertyRequest {
    electricityConnectionType =
        Objects.requireNonNullElse(electricityConnectionType, Optional.empty());
  }
}
