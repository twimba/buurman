package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Property;
import com.buurman.domain.Property.DepreciationMethod;
import com.buurman.domain.Property.MortgageType;

public record PropertyResponse(
    String identifier,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    Property.PropertyStatus status,
    String street,
    String city,
    String postalCode,
    String country,
    @Nullable BigDecimal latitude,
    @Nullable BigDecimal longitude,
    @Nullable String geocodeAccuracy,
    @Nullable BigDecimal areaValue,
    @Nullable String areaUnit,
    @Nullable String mainPhotoUrl,
    @Nullable String mainPhotoThumbnailUrl,

    // Construction & Structure
    @Nullable Integer yearBuilt,
    @Nullable Integer yearLastRenovated,
    @Nullable String constructionType,
    @Nullable String foundationType,
    @Nullable String roofType,
    @Nullable String wallConstruction,
    @Nullable String flooringType,
    @Nullable String windowType,
    @Nullable Integer numberOfFloors,
    @Nullable String structuralNotes,

    // Energy & Climate
    @Nullable String energyEfficiencyRating,
    @Nullable LocalDate energyCertificateExpiryDate,
    @Nullable String heatingType,
    @Nullable String coolingType,
    @Nullable String hotWaterSystem,
    @Nullable String insulationNotes,

    // Utilities & Connections
    @Nullable String electricityConnectionType,
    @Nullable Integer electricityCapacityAmps,
    @Nullable String waterConnectionType,
    @Nullable Boolean hasGasConnection,
    @Nullable String sewageType,
    @Nullable String internetConnectionType,
    @Nullable Integer internetMaxSpeedMbps,
    @Nullable String internetStatus,

    // Parking
    @Nullable Integer parkingSpaces,
    @Nullable String parkingType,

    // Safety & Security
    @Nullable Boolean hasSmokeDetectors,
    @Nullable Boolean hasCoDetectors,
    @Nullable Boolean hasFireExtinguisher,
    @Nullable Boolean hasSprinklerSystem,
    @Nullable Boolean hasAlarmSystem,
    @Nullable Boolean hasSecurityCameras,
    @Nullable Boolean hasSecureEntry,
    @Nullable String safetyNotes,

    // Accessibility
    @Nullable Boolean isWheelchairAccessible,
    @Nullable Boolean hasElevator,
    @Nullable Boolean hasStepFreeEntrance,
    @Nullable Boolean hasAdaptedBathroom,
    @Nullable String accessibilityNotes,

    // Investment & Financial
    @Nullable BigDecimal purchasePrice,
    @Nullable String purchasePriceCurrency,
    @Nullable LocalDate purchaseDate,
    @Nullable BigDecimal currentMarketValue,
    @Nullable String currentMarketValueCurrency,
    @Nullable LocalDate marketValueDate,
    @Nullable Property.MortgageType mortgageType,
    @Nullable BigDecimal mortgageAmount,
    @Nullable String mortgageAmountCurrency,
    @Nullable BigDecimal mortgageInterestRate,
    @Nullable LocalDate mortgageStartDate,
    @Nullable LocalDate mortgageEndDate,
    @Nullable BigDecimal monthlyMortgagePayment,
    @Nullable String monthlyMortgagePaymentCurrency,
    @Nullable BigDecimal annualPropertyTax,
    @Nullable String annualPropertyTaxCurrency,
    @Nullable BigDecimal annualInsurance,
    @Nullable String annualInsuranceCurrency,
    @Nullable BigDecimal annualHoaFee,
    @Nullable String annualHoaFeeCurrency,
    @Nullable BigDecimal annualManagementFee,
    @Nullable String annualManagementFeeCurrency,
    @Nullable BigDecimal annualMaintenanceReserve,
    @Nullable String annualMaintenanceReserveCurrency,
    @Nullable String annualPropertyTaxDueMonth,
    @Nullable String annualInsuranceDueMonth,
    @Nullable String annualHoaFeeDueMonth,
    @Nullable String annualManagementFeeDueMonth,
    @Nullable String annualMaintenanceReserveDueMonth,
    @Nullable Property.DepreciationMethod depreciationMethod,
    @Nullable Integer depreciationYears,
    @Nullable BigDecimal landValue,
    @Nullable String landValueCurrency,

    // Category-specific details (only one is non-null)
    @Nullable ResidentialDetailsResponse residentialDetails,
    @Nullable CommercialDetailsResponse commercialDetails,
    @Nullable IndustrialDetailsResponse industrialDetails,
    @Nullable AgriculturalDetailsResponse agriculturalDetails,

    // Nested collections
    List<PropertyOutdoorAreaResponse> outdoorAreas,
    List<PropertyAmenityResponse> amenities,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
