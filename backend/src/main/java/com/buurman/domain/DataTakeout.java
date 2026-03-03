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
public class DataTakeout {

  public enum TakeoutStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }

  private UUID id;
  private String identifier;
  private UUID teamId;
  private TakeoutStatus status;
  @Builder.Default private int progress = 0;
  @Builder.Default private Optional<String> fileKey = Optional.empty();
  @Builder.Default private Optional<Long> fileSize = Optional.empty();
  @Builder.Default private Optional<String> error = Optional.empty();
  private UUID createdBy;
  private UUID updatedBy;
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<Instant> completedAt = Optional.empty();
  @Builder.Default private Optional<Instant> expiresAt = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
