package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyAmenity {

  private UUID id;
  private UUID propertyId;
  private UUID amenityId;
  private UUID teamId;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant deletedAt;
}
