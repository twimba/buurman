package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyAgriculturalDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private BigDecimal totalLandAreaValue;
  private String totalLandAreaUnit;
  private BigDecimal arableAreaValue;
  private String arableAreaUnit;
  private String soilType;
  private Boolean hasWaterRights;
  private String waterSource;
  private String irrigationType;
  private String fencingType;
  private Boolean hasOutbuildings;
  private String outbuildingDetails;
  private String currentUse;
  private String zoningClassification;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
