package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class Property {

  /**
   * Sentinel value for variable mortgage payments (-1). When monthlyMortgagePayment equals this,
   * the payment varies and should not be used for fixed projections.
   */
  public static final BigDecimal VARIABLE_PAYMENT_SENTINEL = BigDecimal.valueOf(-1);

  public static final long VARIABLE_PAYMENT_SENTINEL_DB = -1L;

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String street;
  private String city;
  private String postalCode;
  private String country;
  private @Nullable BigDecimal latitude;
  private @Nullable BigDecimal longitude;
  private @Nullable String geocodeAccuracy;
  private @Nullable BigDecimal areaValue;
  private @Nullable String areaUnit;
  private PropertyCategory propertyCategory;
  private PropertyType propertyType;
  private PropertyStatus status;

  // Construction & Structure
  private @Nullable Integer yearBuilt;
  private @Nullable Integer yearLastRenovated;
  private @Nullable String constructionType;
  private @Nullable String foundationType;
  private @Nullable String roofType;
  private @Nullable String wallConstruction;
  private @Nullable String flooringType;
  private @Nullable String windowType;
  private @Nullable Integer numberOfFloors;
  private @Nullable String structuralNotes;

  // Energy & Climate
  private @Nullable String energyEfficiencyRating;
  private @Nullable LocalDate energyCertificateExpiryDate;
  private @Nullable String heatingType;
  private @Nullable String coolingType;
  private @Nullable String hotWaterSystem;
  private @Nullable String insulationNotes;

  // Utilities & Connections
  private @Nullable String electricityConnectionType;
  private @Nullable Integer electricityCapacityAmps;
  private @Nullable String waterConnectionType;
  private @Nullable Boolean hasGasConnection;
  private @Nullable String sewageType;
  private @Nullable String internetConnectionType;
  private @Nullable Integer internetMaxSpeedMbps;
  private @Nullable String internetStatus;

  // Parking
  private @Nullable Integer parkingSpaces;
  private @Nullable String parkingType;

  // Safety & Security
  private @Nullable Boolean hasSmokeDetectors;
  private @Nullable Boolean hasCoDetectors;
  private @Nullable Boolean hasFireExtinguisher;
  private @Nullable Boolean hasSprinklerSystem;
  private @Nullable Boolean hasAlarmSystem;
  private @Nullable Boolean hasSecurityCameras;
  private @Nullable Boolean hasSecureEntry;
  private @Nullable String safetyNotes;

  // Accessibility
  private @Nullable Boolean isWheelchairAccessible;
  private @Nullable Boolean hasElevator;
  private @Nullable Boolean hasStepFreeEntrance;
  private @Nullable Boolean hasAdaptedBathroom;
  private @Nullable String accessibilityNotes;

  // Investment & Financial
  private @Nullable BigDecimal purchasePrice;
  private @Nullable String purchasePriceCurrency;
  private @Nullable LocalDate purchaseDate;
  private @Nullable BigDecimal currentMarketValue;
  private @Nullable String currentMarketValueCurrency;
  private @Nullable LocalDate marketValueDate;

  // Mortgage
  private @Nullable MortgageType mortgageType;
  private @Nullable BigDecimal mortgageAmount;
  private @Nullable String mortgageAmountCurrency;
  private @Nullable BigDecimal mortgageInterestRate;
  private @Nullable LocalDate mortgageStartDate;
  private @Nullable LocalDate mortgageEndDate;
  private @Nullable BigDecimal monthlyMortgagePayment;
  private @Nullable String monthlyMortgagePaymentCurrency;

  // Operating Costs (annual)
  private @Nullable BigDecimal annualPropertyTax;
  private @Nullable String annualPropertyTaxCurrency;
  private @Nullable BigDecimal annualInsurance;
  private @Nullable String annualInsuranceCurrency;
  private @Nullable BigDecimal annualHoaFee;
  private @Nullable String annualHoaFeeCurrency;
  private @Nullable BigDecimal annualManagementFee;
  private @Nullable String annualManagementFeeCurrency;
  private @Nullable BigDecimal annualMaintenanceReserve;
  private @Nullable String annualMaintenanceReserveCurrency;

  // Operating Cost Due Months — comma-separated month numbers (e.g. "1,3,7"), null = all months
  private @Nullable String annualPropertyTaxDueMonth;
  private @Nullable String annualInsuranceDueMonth;
  private @Nullable String annualHoaFeeDueMonth;
  private @Nullable String annualManagementFeeDueMonth;
  private @Nullable String annualMaintenanceReserveDueMonth;

  // Depreciation
  private @Nullable DepreciationMethod depreciationMethod;
  private @Nullable Integer depreciationYears;
  private @Nullable BigDecimal landValue;
  private @Nullable String landValueCurrency;

  // Audit
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;

  public enum PropertyCategory {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL,
    AGRICULTURAL,
    MIXED_USE
  }

  public enum PropertyType {
    // Residential
    APARTMENT,
    HOUSE,
    STUDIO,
    ROOM,
    VILLA,
    TOWNHOUSE,
    OTHER_RESIDENTIAL,
    // Commercial
    OFFICE,
    RETAIL,
    RESTAURANT,
    HOTEL,
    SHOWROOM,
    AUTO_DEALERSHIP,
    SNACKBAR,
    CAFE,
    MOTEL,
    BAR,
    BED_AND_BREAKFAST,
    OTHER_COMMERCIAL,
    // Industrial
    WAREHOUSE,
    WORKSHOP,
    FACTORY,
    DATA_CENTER,
    COLD_STORAGE,
    GARAGE,
    OTHER_INDUSTRIAL,
    // Agricultural
    FARMLAND,
    RANCH,
    GREENHOUSE,
    ORCHARD,
    VINEYARD,
    OTHER_AGRICULTURAL,
    // Mixed-Use
    MIXED_USE,
    // Legacy
    COMMERCIAL
  }

  public enum PropertyStatus {
    VACANT,
    OCCUPIED,
    MAINTENANCE,
    UNAVAILABLE,
    UNDER_RENOVATION,
    FALLOW,
    LISTED
  }

  public enum MortgageType {
    FIXED_RATE,
    VARIABLE_RATE,
    INTEREST_ONLY,
    NONE
  }

  public enum DepreciationMethod {
    STRAIGHT_LINE,
    DECLINING_BALANCE,
    NONE
  }
}
