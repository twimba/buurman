package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Property {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String street;
  private String city;
  private String postalCode;
  private String country;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String geocodeAccuracy;
  private BigDecimal areaValue;
  private String areaUnit;
  private PropertyCategory propertyCategory;
  private PropertyType propertyType;
  private PropertyStatus status;

  // Construction & Structure
  private Integer yearBuilt;
  private Integer yearLastRenovated;
  private String constructionType;
  private String foundationType;
  private String roofType;
  private String wallConstruction;
  private String flooringType;
  private String windowType;
  private Integer numberOfFloors;
  private String structuralNotes;

  // Energy & Climate
  private String energyEfficiencyRating;
  private LocalDate energyCertificateExpiryDate;
  private String heatingType;
  private String coolingType;
  private String hotWaterSystem;
  private String insulationNotes;

  // Utilities & Connections
  private String electricityConnectionType;
  private Integer electricityCapacityAmps;
  private String waterConnectionType;
  private Boolean hasGasConnection;
  private String sewageType;
  private String internetConnectionType;
  private Integer internetMaxSpeedMbps;
  private String internetStatus;

  // Parking
  private Integer parkingSpaces;
  private String parkingType;

  // Safety & Security
  private Boolean hasSmokeDetectors;
  private Boolean hasCoDetectors;
  private Boolean hasFireExtinguisher;
  private Boolean hasSprinklerSystem;
  private Boolean hasAlarmSystem;
  private Boolean hasSecurityCameras;
  private Boolean hasSecureEntry;
  private String safetyNotes;

  // Accessibility
  private Boolean isWheelchairAccessible;
  private Boolean hasElevator;
  private Boolean hasStepFreeEntrance;
  private Boolean hasAdaptedBathroom;
  private String accessibilityNotes;

  // Investment & Financial
  private BigDecimal purchasePrice;
  private String purchasePriceCurrency;
  private LocalDate purchaseDate;
  private BigDecimal currentMarketValue;
  private String currentMarketValueCurrency;
  private LocalDate marketValueDate;

  // Mortgage
  private MortgageType mortgageType;
  private BigDecimal mortgageAmount;
  private String mortgageAmountCurrency;
  private BigDecimal mortgageInterestRate;
  private LocalDate mortgageStartDate;
  private LocalDate mortgageEndDate;
  private BigDecimal monthlyMortgagePayment;
  private String monthlyMortgagePaymentCurrency;

  // Operating Costs (annual)
  private BigDecimal annualPropertyTax;
  private String annualPropertyTaxCurrency;
  private BigDecimal annualInsurance;
  private String annualInsuranceCurrency;
  private BigDecimal annualHoaFee;
  private String annualHoaFeeCurrency;
  private BigDecimal annualManagementFee;
  private String annualManagementFeeCurrency;
  private BigDecimal annualMaintenanceReserve;
  private String annualMaintenanceReserveCurrency;

  // Operating Cost Due Months — comma-separated month numbers (e.g. "1,3,7"), null = all months
  private String annualPropertyTaxDueMonth;
  private String annualInsuranceDueMonth;
  private String annualHoaFeeDueMonth;
  private String annualManagementFeeDueMonth;
  private String annualMaintenanceReserveDueMonth;

  // Depreciation
  private DepreciationMethod depreciationMethod;
  private Integer depreciationYears;
  private BigDecimal landValue;
  private String landValueCurrency;

  // Audit
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant deletedAt;

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
