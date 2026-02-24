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
public class Tenant {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String firstName;
  @Builder.Default private Optional<String> lastName = Optional.empty();
  @Builder.Default private Optional<String> email = Optional.empty();
  @Builder.Default private Optional<String> phone = Optional.empty();
  @Builder.Default private Optional<String> taxNumber = Optional.empty();
  @Builder.Default private Optional<String> idNumber = Optional.empty();
  @Builder.Default private Optional<String> additionalInfo = Optional.empty();
  @Builder.Default private Optional<UUID> currentPropertyId = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
