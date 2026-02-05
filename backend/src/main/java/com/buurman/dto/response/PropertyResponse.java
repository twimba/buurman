package com.buurman.dto.response;

import com.buurman.domain.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PropertyResponse(
        String identifier,
        String street,
        String city,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal areaValue,
        String areaUnit,
        Property.PropertyType propertyType,
        Property.PropertyStatus status,
        String mainPhotoUrl,

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

        // Nested collections
        List<PropertyOutdoorAreaResponse> outdoorAreas,
        List<PropertyAmenityResponse> amenities,

        Instant createdAt,
        Instant updatedAt
) {}
