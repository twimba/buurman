package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal areaValue,
    String areaUnit,
    String mainPhotoUrl,
    String mainPhotoThumbnailUrl,

    // Construction & Structure
    Integer yearBuilt,
    Integer yearLastRenovated,
    String constructionType,
    String foundationType,
    String roofType,
    String wallConstruction,
    String flooringType,
    String windowType,
    Integer numberOfFloors,
    String structuralNotes,

    // Energy & Climate
    String energyEfficiencyRating,
    LocalDate energyCertificateExpiryDate,
    String heatingType,
    String coolingType,
    String hotWaterSystem,
    String insulationNotes,

    // Utilities & Connections
    String electricityConnectionType,
    Integer electricityCapacityAmps,
    String waterConnectionType,
    Boolean hasGasConnection,
    String sewageType,
    String internetConnectionType,
    Integer internetMaxSpeedMbps,
    String internetStatus,

    // Parking
    Integer parkingSpaces,
    String parkingType,

    // Safety & Security
    Boolean hasSmokeDetectors,
    Boolean hasCoDetectors,
    Boolean hasFireExtinguisher,
    Boolean hasSprinklerSystem,
    Boolean hasAlarmSystem,
    Boolean hasSecurityCameras,
    Boolean hasSecureEntry,
    String safetyNotes,

    // Accessibility
    Boolean isWheelchairAccessible,
    Boolean hasElevator,
    Boolean hasStepFreeEntrance,
    Boolean hasAdaptedBathroom,
    String accessibilityNotes,

    // Category-specific details (only one is non-null)
    ResidentialDetailsResponse residentialDetails,
    CommercialDetailsResponse commercialDetails,
    IndustrialDetailsResponse industrialDetails,
    AgriculturalDetailsResponse agriculturalDetails,

    // Nested collections
    List<PropertyOutdoorAreaResponse> outdoorAreas,
    List<PropertyAmenityResponse> amenities,
    Instant createdAt,
    Instant updatedAt) {}
