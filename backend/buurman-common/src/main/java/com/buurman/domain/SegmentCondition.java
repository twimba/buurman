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
public class SegmentCondition {

  private UUID id;
  private UUID segmentId;
  private SegmentAttribute attribute;
  private SegmentOperator operator;
  private String value;
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
}
