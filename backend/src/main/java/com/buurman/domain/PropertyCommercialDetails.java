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
public class PropertyCommercialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private @Nullable BigDecimal usableAreaValue;
  private @Nullable String usableAreaUnit;
  private @Nullable BigDecimal commonAreaValue;
  private @Nullable String commonAreaUnit;
  private @Nullable Integer floorLevel;
  private @Nullable BigDecimal ceilingHeightM;
  private @Nullable Boolean hasStorefront;
  private @Nullable Boolean hasSignageRights;
  private @Nullable String zoningClassification;
  private @Nullable Integer maxOccupancy;
  private @Nullable Integer restroomCount;
  private @Nullable Boolean hasKitchenFacility;
  private @Nullable Boolean accessibilityCompliant;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
