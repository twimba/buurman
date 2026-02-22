package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class PropertyAgriculturalDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private @Nullable BigDecimal totalLandAreaValue;
  private @Nullable String totalLandAreaUnit;
  private @Nullable BigDecimal arableAreaValue;
  private @Nullable String arableAreaUnit;
  private @Nullable String soilType;
  private @Nullable Boolean hasWaterRights;
  private @Nullable String waterSource;
  private @Nullable String irrigationType;
  private @Nullable String fencingType;
  private @Nullable Boolean hasOutbuildings;
  private @Nullable String outbuildingDetails;
  private @Nullable String currentUse;
  private @Nullable String zoningClassification;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
