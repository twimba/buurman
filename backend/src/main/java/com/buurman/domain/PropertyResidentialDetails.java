package com.buurman.domain;

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
public class PropertyResidentialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  @Builder.Default private Optional<Integer> bedrooms = Optional.empty();
  @Builder.Default private Optional<Integer> bathrooms = Optional.empty();
  @Builder.Default private Optional<Boolean> furnished = Optional.empty();
  @Builder.Default private Optional<String> petPolicy = Optional.empty();
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
