package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PropertyResponse(
    Sid identifier,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    AllocationBasis allocationBasis,
    String street,
    String city,
    String postalCode,
    String countryCode,
    Optional<String> regionCode,
    Optional<BigDecimal> latitude,
    Optional<BigDecimal> longitude,
    Optional<String> geocodeAccuracy,
    Optional<String> mainPhotoUrl,
    Optional<String> mainPhotoThumbnailUrl,

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

    // Category-specific details (only one is non-null)
    Optional<ResidentialDetailsResponse> residentialDetails,
    Optional<CommercialDetailsResponse> commercialDetails,
    Optional<IndustrialDetailsResponse> industrialDetails,
    Optional<AgriculturalDetailsResponse> agriculturalDetails,

    // Nested collections
    Optional<List<PropertyOutdoorAreaResponse>> outdoorAreas,
    Optional<List<PropertyAmenityResponse>> amenities,

    // Unit facts — properties.status was dropped in V068; these are the replacement. A property
    // is never created without at least one unit (see PropertyService#createProperty), so
    // unitCount is always >= 1 for a property returned by this API.
    int unitCount,
    int occupiedUnitCount,
    int vacantUnitCount,
    List<UnitSummaryResponse> units,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
