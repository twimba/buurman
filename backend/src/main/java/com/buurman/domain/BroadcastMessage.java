package com.buurman.domain;

import java.time.Instant;
import java.util.List;
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
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private String title;
  private String body;
  private String severity;
  @Builder.Default private String scope = "GLOBAL";
  private Instant startAt;
  @Builder.Default private Optional<Instant> endAt = Optional.empty();
  private boolean showOnLogin;
  private boolean showOnRegister;
  private boolean showInApp;
  @Builder.Default private List<UUID> targetTeamIds = List.of();
  @Builder.Default private List<UUID> targetUserIds = List.of();
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
}
