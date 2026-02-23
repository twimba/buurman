package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    Optional<Integer> electricityCapacityAmps,
    Optional<String> waterConnectionType,
    Optional<Boolean> hasGasConnection,
    Optional<String> sewageType,
    Optional<String> internetConnectionType,
    Optional<Integer> internetMaxSpeedMbps,
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

    // Investment & Financial
    Optional<BigDecimal> purchasePrice,
    Optional<String> purchasePriceCurrency,
    Optional<LocalDate> purchaseDate,
    Optional<BigDecimal> currentMarketValue,
    Optional<String> currentMarketValueCurrency,
    Optional<LocalDate> marketValueDate,
    Optional<MortgageType> mortgageType,
    Optional<BigDecimal> mortgageAmount,
    Optional<String> mortgageAmountCurrency,
    Optional<BigDecimal> mortgageInterestRate,
    Optional<LocalDate> mortgageStartDate,
    Optional<LocalDate> mortgageEndDate,
    Optional<BigDecimal> monthlyMortgagePayment,
    Optional<String> monthlyMortgagePaymentCurrency,
    Optional<BigDecimal> annualPropertyTax,
    Optional<String> annualPropertyTaxCurrency,
    Optional<BigDecimal> annualInsurance,
    Optional<String> annualInsuranceCurrency,
    Optional<BigDecimal> annualHoaFee,
    Optional<String> annualHoaFeeCurrency,
    Optional<BigDecimal> annualManagementFee,
    Optional<String> annualManagementFeeCurrency,
    Optional<BigDecimal> annualMaintenanceReserve,
    Optional<String> annualMaintenanceReserveCurrency,
    Optional<String> annualPropertyTaxDueMonth,
    Optional<String> annualInsuranceDueMonth,
    Optional<String> annualHoaFeeDueMonth,
    Optional<String> annualManagementFeeDueMonth,
    Optional<String> annualMaintenanceReserveDueMonth,
    Optional<DepreciationMethod> depreciationMethod,
    Optional<Integer> depreciationYears,
    Optional<BigDecimal> landValue,
    Optional<String> landValueCurrency,

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
