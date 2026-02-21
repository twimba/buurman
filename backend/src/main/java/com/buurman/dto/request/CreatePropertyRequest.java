package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    @NotBlank(message = "Country is required") String country,
    BigDecimal latitude,
    BigDecimal longitude,
    String geocodeAccuracy,
    @Positive(message = "Area value must be positive") BigDecimal areaValue,
    String areaUnit,

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

    // Investment & Financial
    BigDecimal purchasePrice,
    String purchasePriceCurrency,
    LocalDate purchaseDate,
    BigDecimal currentMarketValue,
    String currentMarketValueCurrency,
    LocalDate marketValueDate,
    Property.MortgageType mortgageType,
    BigDecimal mortgageAmount,
    String mortgageAmountCurrency,
    BigDecimal mortgageInterestRate,
    LocalDate mortgageStartDate,
    LocalDate mortgageEndDate,
    BigDecimal monthlyMortgagePayment,
    String monthlyMortgagePaymentCurrency,
    BigDecimal annualPropertyTax,
    String annualPropertyTaxCurrency,
    BigDecimal annualInsurance,
    String annualInsuranceCurrency,
    BigDecimal annualHoaFee,
    String annualHoaFeeCurrency,
    BigDecimal annualManagementFee,
    String annualManagementFeeCurrency,
    BigDecimal annualMaintenanceReserve,
    String annualMaintenanceReserveCurrency,
    Integer annualPropertyTaxDueMonth,
    Integer annualInsuranceDueMonth,
    Integer annualHoaFeeDueMonth,
    Integer annualManagementFeeDueMonth,
    Integer annualMaintenanceReserveDueMonth,
    Property.DepreciationMethod depreciationMethod,
    Integer depreciationYears,
    BigDecimal landValue,
    String landValueCurrency,

    // Category-specific details (only one should be provided)
    @Valid ResidentialDetailsRequest residentialDetails,
    @Valid CommercialDetailsRequest commercialDetails,
    @Valid IndustrialDetailsRequest industrialDetails,
    @Valid AgriculturalDetailsRequest agriculturalDetails) {}
