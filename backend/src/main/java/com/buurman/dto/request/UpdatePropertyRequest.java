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

public record UpdatePropertyRequest(
    @NotNull(message = "Property type is required") Property.PropertyType propertyType,
    @NotNull(message = "Status is required") Property.PropertyStatus status,
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @NotBlank(message = "Postal code is required") String postalCode,
    @NotBlank(message = "Country is required") String country,
    Optional<BigDecimal> latitude,
    Optional<BigDecimal> longitude,
    Optional<String> geocodeAccuracy,
    @Positive(message = "Area value must be positive") Optional<BigDecimal> areaValue,
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
    Optional<Property.MortgageType> mortgageType,
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
    Optional<Property.DepreciationMethod> depreciationMethod,
    Optional<Integer> depreciationYears,
    Optional<BigDecimal> landValue,
    Optional<String> landValueCurrency,

    // Category-specific details (only matching category should be provided)
    // Note: propertyCategory is NOT here — it's immutable
    @Valid Optional<ResidentialDetailsRequest> residentialDetails,
    @Valid Optional<CommercialDetailsRequest> commercialDetails,
    @Valid Optional<IndustrialDetailsRequest> industrialDetails,
    @Valid Optional<AgriculturalDetailsRequest> agriculturalDetails) {

  public UpdatePropertyRequest {
    latitude = Objects.requireNonNullElse(latitude, Optional.empty());
    longitude = Objects.requireNonNullElse(longitude, Optional.empty());
    geocodeAccuracy = Objects.requireNonNullElse(geocodeAccuracy, Optional.empty());
    areaValue = Objects.requireNonNullElse(areaValue, Optional.empty());
    areaUnit = Objects.requireNonNullElse(areaUnit, Optional.empty());
    yearBuilt = Objects.requireNonNullElse(yearBuilt, Optional.empty());
    yearLastRenovated = Objects.requireNonNullElse(yearLastRenovated, Optional.empty());
    constructionType = Objects.requireNonNullElse(constructionType, Optional.empty());
    foundationType = Objects.requireNonNullElse(foundationType, Optional.empty());
    roofType = Objects.requireNonNullElse(roofType, Optional.empty());
    wallConstruction = Objects.requireNonNullElse(wallConstruction, Optional.empty());
    flooringType = Objects.requireNonNullElse(flooringType, Optional.empty());
    windowType = Objects.requireNonNullElse(windowType, Optional.empty());
    numberOfFloors = Objects.requireNonNullElse(numberOfFloors, Optional.empty());
    structuralNotes = Objects.requireNonNullElse(structuralNotes, Optional.empty());
    energyEfficiencyRating = Objects.requireNonNullElse(energyEfficiencyRating, Optional.empty());
    energyCertificateExpiryDate =
        Objects.requireNonNullElse(energyCertificateExpiryDate, Optional.empty());
    heatingType = Objects.requireNonNullElse(heatingType, Optional.empty());
    coolingType = Objects.requireNonNullElse(coolingType, Optional.empty());
    hotWaterSystem = Objects.requireNonNullElse(hotWaterSystem, Optional.empty());
    insulationNotes = Objects.requireNonNullElse(insulationNotes, Optional.empty());
    electricityConnectionType =
        Objects.requireNonNullElse(electricityConnectionType, Optional.empty());
    electricityCapacityAmps = Objects.requireNonNullElse(electricityCapacityAmps, Optional.empty());
    waterConnectionType = Objects.requireNonNullElse(waterConnectionType, Optional.empty());
    hasGasConnection = Objects.requireNonNullElse(hasGasConnection, Optional.empty());
    sewageType = Objects.requireNonNullElse(sewageType, Optional.empty());
    internetConnectionType = Objects.requireNonNullElse(internetConnectionType, Optional.empty());
    internetMaxSpeedMbps = Objects.requireNonNullElse(internetMaxSpeedMbps, Optional.empty());
    internetStatus = Objects.requireNonNullElse(internetStatus, Optional.empty());
    parkingSpaces = Objects.requireNonNullElse(parkingSpaces, Optional.empty());
    parkingType = Objects.requireNonNullElse(parkingType, Optional.empty());
    hasSmokeDetectors = Objects.requireNonNullElse(hasSmokeDetectors, Optional.empty());
    hasCoDetectors = Objects.requireNonNullElse(hasCoDetectors, Optional.empty());
    hasFireExtinguisher = Objects.requireNonNullElse(hasFireExtinguisher, Optional.empty());
    hasSprinklerSystem = Objects.requireNonNullElse(hasSprinklerSystem, Optional.empty());
    hasAlarmSystem = Objects.requireNonNullElse(hasAlarmSystem, Optional.empty());
    hasSecurityCameras = Objects.requireNonNullElse(hasSecurityCameras, Optional.empty());
    hasSecureEntry = Objects.requireNonNullElse(hasSecureEntry, Optional.empty());
    safetyNotes = Objects.requireNonNullElse(safetyNotes, Optional.empty());
    isWheelchairAccessible = Objects.requireNonNullElse(isWheelchairAccessible, Optional.empty());
    hasElevator = Objects.requireNonNullElse(hasElevator, Optional.empty());
    hasStepFreeEntrance = Objects.requireNonNullElse(hasStepFreeEntrance, Optional.empty());
    hasAdaptedBathroom = Objects.requireNonNullElse(hasAdaptedBathroom, Optional.empty());
    accessibilityNotes = Objects.requireNonNullElse(accessibilityNotes, Optional.empty());
    purchasePrice = Objects.requireNonNullElse(purchasePrice, Optional.empty());
    purchasePriceCurrency = Objects.requireNonNullElse(purchasePriceCurrency, Optional.empty());
    purchaseDate = Objects.requireNonNullElse(purchaseDate, Optional.empty());
    currentMarketValue = Objects.requireNonNullElse(currentMarketValue, Optional.empty());
    currentMarketValueCurrency =
        Objects.requireNonNullElse(currentMarketValueCurrency, Optional.empty());
    marketValueDate = Objects.requireNonNullElse(marketValueDate, Optional.empty());
    mortgageType = Objects.requireNonNullElse(mortgageType, Optional.empty());
    mortgageAmount = Objects.requireNonNullElse(mortgageAmount, Optional.empty());
    mortgageAmountCurrency = Objects.requireNonNullElse(mortgageAmountCurrency, Optional.empty());
    mortgageInterestRate = Objects.requireNonNullElse(mortgageInterestRate, Optional.empty());
    mortgageStartDate = Objects.requireNonNullElse(mortgageStartDate, Optional.empty());
    mortgageEndDate = Objects.requireNonNullElse(mortgageEndDate, Optional.empty());
    monthlyMortgagePayment = Objects.requireNonNullElse(monthlyMortgagePayment, Optional.empty());
    monthlyMortgagePaymentCurrency =
        Objects.requireNonNullElse(monthlyMortgagePaymentCurrency, Optional.empty());
    annualPropertyTax = Objects.requireNonNullElse(annualPropertyTax, Optional.empty());
    annualPropertyTaxCurrency =
        Objects.requireNonNullElse(annualPropertyTaxCurrency, Optional.empty());
    annualInsurance = Objects.requireNonNullElse(annualInsurance, Optional.empty());
    annualInsuranceCurrency = Objects.requireNonNullElse(annualInsuranceCurrency, Optional.empty());
    annualHoaFee = Objects.requireNonNullElse(annualHoaFee, Optional.empty());
    annualHoaFeeCurrency = Objects.requireNonNullElse(annualHoaFeeCurrency, Optional.empty());
    annualManagementFee = Objects.requireNonNullElse(annualManagementFee, Optional.empty());
    annualManagementFeeCurrency =
        Objects.requireNonNullElse(annualManagementFeeCurrency, Optional.empty());
    annualMaintenanceReserve =
        Objects.requireNonNullElse(annualMaintenanceReserve, Optional.empty());
    annualMaintenanceReserveCurrency =
        Objects.requireNonNullElse(annualMaintenanceReserveCurrency, Optional.empty());
    annualPropertyTaxDueMonth =
        Objects.requireNonNullElse(annualPropertyTaxDueMonth, Optional.empty());
    annualInsuranceDueMonth = Objects.requireNonNullElse(annualInsuranceDueMonth, Optional.empty());
    annualHoaFeeDueMonth = Objects.requireNonNullElse(annualHoaFeeDueMonth, Optional.empty());
    annualManagementFeeDueMonth =
        Objects.requireNonNullElse(annualManagementFeeDueMonth, Optional.empty());
    annualMaintenanceReserveDueMonth =
        Objects.requireNonNullElse(annualMaintenanceReserveDueMonth, Optional.empty());
    depreciationMethod = Objects.requireNonNullElse(depreciationMethod, Optional.empty());
    depreciationYears = Objects.requireNonNullElse(depreciationYears, Optional.empty());
    landValue = Objects.requireNonNullElse(landValue, Optional.empty());
    landValueCurrency = Objects.requireNonNullElse(landValueCurrency, Optional.empty());
    residentialDetails = Objects.requireNonNullElse(residentialDetails, Optional.empty());
    commercialDetails = Objects.requireNonNullElse(commercialDetails, Optional.empty());
    industrialDetails = Objects.requireNonNullElse(industrialDetails, Optional.empty());
    agriculturalDetails = Objects.requireNonNullElse(agriculturalDetails, Optional.empty());
  }
}
