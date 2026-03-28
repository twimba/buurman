package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FeatureFlag {

  private UUID id;
  private String key;
  private String valueType;
  @Builder.Default private boolean defaultEnabled = false;
  @Builder.Default private Optional<String> defaultValue = Optional.empty();
  @Builder.Default private Optional<String> description = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
