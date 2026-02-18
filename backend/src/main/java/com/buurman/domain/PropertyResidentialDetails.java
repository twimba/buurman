package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyResidentialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private Integer bedrooms;
  private Integer bathrooms;
  private Boolean furnished;
  private String petPolicy;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;

  public enum PetPolicy {
    ALLOWED,
    NOT_ALLOWED,
    NEGOTIABLE
  }
}
