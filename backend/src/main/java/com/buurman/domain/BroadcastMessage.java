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
public class BroadcastMessage {

  private UUID id;
  private String identifier;
  private String title;
  private String body;
  private String severity;
  private Instant startAt;
  @Builder.Default private Optional<Instant> endAt = Optional.empty();
  private boolean showOnLogin;
  private boolean showOnRegister;
  private boolean showInApp;
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
}
