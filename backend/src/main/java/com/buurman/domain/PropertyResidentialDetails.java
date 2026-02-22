package com.buurman.domain;

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
public class PropertyResidentialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  private @Nullable Integer bedrooms;
  private @Nullable Integer bathrooms;
  private @Nullable Boolean furnished;
  private @Nullable String petPolicy;
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
