package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyCommercialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private BigDecimal usableAreaValue;
  private String usableAreaUnit;
  private BigDecimal commonAreaValue;
  private String commonAreaUnit;
  private Integer floorLevel;
  private BigDecimal ceilingHeightM;
  private Boolean hasStorefront;
  private Boolean hasSignageRights;
  private String zoningClassification;
  private Integer maxOccupancy;
  private Integer restroomCount;
  private Boolean hasKitchenFacility;
  private Boolean accessibilityCompliant;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
