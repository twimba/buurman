package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.Property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePropertyRequest(
    @NotNull(message = "Property category is required") Property.PropertyCategory propertyCategory,
    @NotNull(message = "Property type is required") Property.PropertyType propertyType,
    @NotNull(message = "Status is required") Property.PropertyStatus status,
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @NotBlank(message = "Postal code is required") String postalCode,
    @NotBlank(message = "Country code is required") String countryCode,
    Optional<BigDecimal> latitude,
    Optional<BigDecimal> longitude,
    Optional<String> geocodeAccuracy,
    Optional<@Positive(message = "Area value must be positive") BigDecimal> areaValue,
    Optional<String> areaUnit,

    // Construction & Structure
    Optional<Integer> yearBuilt,
    Optional<Integer> yearLastRenovated,
    Optional<String> constructionType,
    Optional<String> foundationType,
    Optional<String> roofType,
    Optional<String> wallConstruction,
    Optional<String> flooringType,
    Optional<String> windowType,
    Optional<Integer> numberOfFloors,
    Optional<String> structuralNotes,

    // Energy & Climate
    Optional<String> energyEfficiencyRating,
    Optional<LocalDate> energyCertificateExpiryDate,
    Optional<String> heatingType,
    Optional<String> coolingType,
    Optional<String> hotWaterSystem,
    Optional<String> insulationNotes,

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
    Optional<Boolean> hasSmokeDetectors,
    Optional<Boolean> hasCoDetectors,
    Optional<Boolean> hasFireExtinguisher,
    Optional<Boolean> hasSprinklerSystem,
    Optional<Boolean> hasAlarmSystem,
    Optional<Boolean> hasSecurityCameras,
    Optional<Boolean> hasSecureEntry,
    Optional<String> safetyNotes,

    // Accessibility
    Optional<Boolean> isWheelchairAccessible,
    Optional<Boolean> hasElevator,
    Optional<Boolean> hasStepFreeEntrance,
    Optional<Boolean> hasAdaptedBathroom,
    Optional<String> accessibilityNotes,

    // Category-specific details (only one should be provided)
    @Valid Optional<ResidentialDetailsRequest> residentialDetails,
    @Valid Optional<CommercialDetailsRequest> commercialDetails,
    @Valid Optional<IndustrialDetailsRequest> industrialDetails,
    @Valid Optional<AgriculturalDetailsRequest> agriculturalDetails) {

  public CreatePropertyRequest {
    energyCertificateExpiryDate =
        Objects.requireNonNullElse(energyCertificateExpiryDate, Optional.empty());
    electricityConnectionType =
        Objects.requireNonNullElse(electricityConnectionType, Optional.empty());
  }
}
