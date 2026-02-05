package com.buurman.dto.request;

import com.buurman.domain.Property;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePropertyRequest(
        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "Country is required")
        String country,

        BigDecimal latitude,
        BigDecimal longitude,

        @Min(value = 0, message = "Bedrooms must be non-negative")
        Integer bedrooms,

        @Min(value = 0, message = "Bathrooms must be non-negative")
        Integer bathrooms,

        @Positive(message = "Area value must be positive")
        BigDecimal areaValue,

        String areaUnit,

        @NotNull(message = "Property type is required")
        Property.PropertyType propertyType,

        @NotNull(message = "Status is required")
        Property.PropertyStatus status,

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
        String accessibilityNotes
) {}
