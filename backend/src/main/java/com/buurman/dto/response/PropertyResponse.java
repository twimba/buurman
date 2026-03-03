package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Property;

public record PropertyResponse(
    String identifier,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    Property.PropertyStatus status,
    String street,
    String city,
    String postalCode,
    String country,
    Optional<BigDecimal> latitude,
    Optional<BigDecimal> longitude,
    Optional<String> geocodeAccuracy,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit,
    Optional<String> mainPhotoUrl,
    Optional<String> mainPhotoThumbnailUrl,

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

    // Category-specific details (only one is non-null)
    Optional<ResidentialDetailsResponse> residentialDetails,
    Optional<CommercialDetailsResponse> commercialDetails,
    Optional<IndustrialDetailsResponse> industrialDetails,
    Optional<AgriculturalDetailsResponse> agriculturalDetails,

    // Nested collections
    Optional<List<PropertyOutdoorAreaResponse>> outdoorAreas,
    Optional<List<PropertyAmenityResponse>> amenities,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
