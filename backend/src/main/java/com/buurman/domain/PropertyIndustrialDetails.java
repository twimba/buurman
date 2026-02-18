package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyIndustrialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private BigDecimal clearHeightM;
  private Integer loadingDocks;
  private Integer driveInDoors;
  private BigDecimal floorLoadCapacityKgSqm;
  private Integer powerCapacityKva;
  private Boolean hasThreePhasePower;
  private Boolean hasCrane;
  private BigDecimal craneCapacityTons;
  private Boolean hasHazmatCertification;
  private Boolean hasVentilationSystem;
  private Boolean hasClimateControl;
  private BigDecimal yardAreaValue;
  private String yardAreaUnit;
  private String zoningClassification;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
