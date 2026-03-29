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
public class FeatureFlagOverride {

  private UUID id;
  private String flagKey;
  private OverrideScope scope;
  @Builder.Default private Optional<String> segmentKey = Optional.empty();
  @Builder.Default private Optional<UUID> teamId = Optional.empty();
  @Builder.Default private Optional<UUID> userId = Optional.empty();
  private boolean enabled;
  @Builder.Default private Optional<String> value = Optional.empty();
  @Builder.Default private int priority = 0;
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
