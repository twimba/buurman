package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bedroom/bathroom/furnishing details for an APARTMENT {@link Unit}. One row per unit
 * (upsert-only, no soft delete): a landlord who changes a unit's type away from APARTMENT and
 * back must not lose this data, so it is intentionally never deleted, only hidden from callers
 * while the unit's type is not APARTMENT (see {@code UnitResidentialDetailsService}).
 */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResidentialDetails {

  private UUID id;
  private UUID unitId;
  private UUID teamId;
  @Builder.Default private Optional<Integer> bedrooms = Optional.empty();
  @Builder.Default private Optional<Integer> bathrooms = Optional.empty();
  @Builder.Default private boolean furnished = false;
  @Builder.Default private Optional<String> petPolicy = Optional.empty();
  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
}
