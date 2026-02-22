package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyIndustrialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private @Nullable BigDecimal clearHeightM;
  private @Nullable Integer loadingDocks;
  private @Nullable Integer driveInDoors;
  private @Nullable BigDecimal floorLoadCapacityKgSqm;
  private @Nullable Integer powerCapacityKva;
  private @Nullable Boolean hasThreePhasePower;
  private @Nullable Boolean hasCrane;
  private @Nullable BigDecimal craneCapacityTons;
  private @Nullable Boolean hasHazmatCertification;
  private @Nullable Boolean hasVentilationSystem;
  private @Nullable Boolean hasClimateControl;
  private @Nullable BigDecimal yardAreaValue;
  private @Nullable String yardAreaUnit;
  private @Nullable String zoningClassification;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
