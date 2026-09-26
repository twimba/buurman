package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Links an {@link Amenity} from the shared catalogue to a {@link Unit}. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitAmenity {

  private UUID id;
  private UUID unitId;
  private UUID amenityId;
  private UUID teamId;
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
