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

public record CreatePropertyRequest(
    @NotNull(message = "Property category is required") Property.PropertyCategory propertyCategory,
    @NotNull(message = "Property type is required") Property.PropertyType propertyType,
    @NotNull(message = "Status is required") Property.PropertyStatus status,
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @NotBlank(message = "Postal code is required") String postalCode,
    @NotBlank(message = "Country is required") String country,
    Optional<BigDecimal> latitude,
    Optional<BigDecimal> longitude,
    Optional<String> geocodeAccuracy,
    Optional<@Positive(message = "Area value must be positive") BigDecimal> areaValue,
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
    Optional<Boolean> mortgagePaymentVariable,
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

    // Category-specific details (only one should be provided)
    @Valid Optional<ResidentialDetailsRequest> residentialDetails,
    @Valid Optional<CommercialDetailsRequest> commercialDetails,
    @Valid Optional<IndustrialDetailsRequest> industrialDetails,
    @Valid Optional<AgriculturalDetailsRequest> agriculturalDetails) {

  public CreatePropertyRequest {
    energyCertificateExpiryDate =
        Objects.requireNonNullElse(energyCertificateExpiryDate, Optional.empty());
    electricityConnectionType =
        Objects.requireNonNullElse(electricityConnectionType, Optional.empty());
    currentMarketValueCurrency =
        Objects.requireNonNullElse(currentMarketValueCurrency, Optional.empty());
    mortgagePaymentVariable = Objects.requireNonNullElse(mortgagePaymentVariable, Optional.empty());
    monthlyMortgagePaymentCurrency =
        Objects.requireNonNullElse(monthlyMortgagePaymentCurrency, Optional.empty());
    annualPropertyTaxCurrency =
        Objects.requireNonNullElse(annualPropertyTaxCurrency, Optional.empty());
    annualManagementFeeCurrency =
        Objects.requireNonNullElse(annualManagementFeeCurrency, Optional.empty());
    annualMaintenanceReserve =
        Objects.requireNonNullElse(annualMaintenanceReserve, Optional.empty());
    annualMaintenanceReserveCurrency =
        Objects.requireNonNullElse(annualMaintenanceReserveCurrency, Optional.empty());
    annualPropertyTaxDueMonth =
        Objects.requireNonNullElse(annualPropertyTaxDueMonth, Optional.empty());
    annualManagementFeeDueMonth =
        Objects.requireNonNullElse(annualManagementFeeDueMonth, Optional.empty());
    annualMaintenanceReserveDueMonth =
        Objects.requireNonNullElse(annualMaintenanceReserveDueMonth, Optional.empty());
  }
}
