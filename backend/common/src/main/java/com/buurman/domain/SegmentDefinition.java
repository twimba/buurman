package com.buurman.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SegmentDefinition {

  private UUID id;
  private String key;
  private String name;
  @Builder.Default private Optional<String> description = Optional.empty();
  @Builder.Default private int priority = 0;
  @Builder.Default private List<SegmentCondition> conditions = List.of();
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
