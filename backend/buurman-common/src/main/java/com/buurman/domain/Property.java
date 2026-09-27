package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private String street;
  private String city;
  private String postalCode;
  private String countryCode;
  @Builder.Default private Optional<String> regionCode = Optional.empty();
  @Builder.Default private Optional<BigDecimal> latitude = Optional.empty();
  @Builder.Default private Optional<BigDecimal> longitude = Optional.empty();
  @Builder.Default private Optional<String> geocodeAccuracy = Optional.empty();
  private PropertyCategory propertyCategory;
  private PropertyType propertyType;
  @Builder.Default private AllocationBasis allocationBasis = AllocationBasis.EQUAL;

  // Construction & Structure
  @Builder.Default private Optional<Integer> yearBuilt = Optional.empty();
  @Builder.Default private Optional<Integer> yearLastRenovated = Optional.empty();
  @Builder.Default private Optional<String> constructionType = Optional.empty();
  @Builder.Default private Optional<String> foundationType = Optional.empty();
  @Builder.Default private Optional<String> roofType = Optional.empty();
  @Builder.Default private Optional<String> wallConstruction = Optional.empty();
  @Builder.Default private Optional<Integer> numberOfFloors = Optional.empty();
  @Builder.Default private Optional<String> structuralNotes = Optional.empty();

  // Utilities & Connections
  @Builder.Default private Optional<String> electricityConnectionType = Optional.empty();
  @Builder.Default private Optional<Integer> electricityCapacityValue = Optional.empty();
  @Builder.Default private Optional<String> electricityCapacityUnit = Optional.empty();
  @Builder.Default private Optional<String> waterConnectionType = Optional.empty();
  @Builder.Default private Optional<Boolean> hasGasConnection = Optional.empty();
  @Builder.Default private Optional<String> sewageType = Optional.empty();
  @Builder.Default private Optional<String> internetConnectionType = Optional.empty();
  @Builder.Default private Optional<Integer> internetMaxSpeedValue = Optional.empty();
  @Builder.Default private Optional<String> internetMaxSpeedUnit = Optional.empty();
  @Builder.Default private Optional<String> internetStatus = Optional.empty();

  // Parking
  @Builder.Default private Optional<Integer> parkingSpaces = Optional.empty();
  @Builder.Default private Optional<String> parkingType = Optional.empty();

  // Safety & Security
  @Builder.Default private Optional<Boolean> hasSprinklerSystem = Optional.empty();
  @Builder.Default private Optional<Boolean> hasAlarmSystem = Optional.empty();
  @Builder.Default private Optional<Boolean> hasSecurityCameras = Optional.empty();
  @Builder.Default private Optional<Boolean> hasSecureEntry = Optional.empty();
  @Builder.Default private Optional<String> safetyNotes = Optional.empty();

  // Accessibility
  @Builder.Default private Optional<Boolean> isWheelchairAccessible = Optional.empty();
  @Builder.Default private Optional<Boolean> hasElevator = Optional.empty();
  @Builder.Default private Optional<Boolean> hasStepFreeEntrance = Optional.empty();

  // Audit
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();

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
}
