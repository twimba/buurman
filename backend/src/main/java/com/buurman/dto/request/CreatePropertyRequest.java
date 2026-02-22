package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Property;
import com.buurman.domain.Property.DepreciationMethod;
import com.buurman.domain.Property.MortgageType;

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
    @Nullable BigDecimal latitude,
    @Nullable BigDecimal longitude,
    @Nullable String geocodeAccuracy,
    @Nullable @Positive(message = "Area value must be positive") BigDecimal areaValue,
    @Nullable String areaUnit,

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

    // Category-specific details (only one should be provided)
    @Nullable @Valid ResidentialDetailsRequest residentialDetails,
    @Nullable @Valid CommercialDetailsRequest commercialDetails,
    @Nullable @Valid IndustrialDetailsRequest industrialDetails,
    @Nullable @Valid AgriculturalDetailsRequest agriculturalDetails) {}
