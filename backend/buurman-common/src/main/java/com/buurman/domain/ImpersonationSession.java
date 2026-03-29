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
public class ImpersonationSession {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID adminUserId;
  private String adminEmail;
  private String adminName;
  private UUID targetUserId;
  private UUID targetTeamId;
  private UUID sessionToken;
  private ImpersonationMode mode;
  private String reason;
  private ImpersonationStatus status;
  @Builder.Default private Optional<String> ipAddress = Optional.empty();
  private Instant createdAt;
  @Builder.Default private Optional<Instant> activatedAt = Optional.empty();
  private Instant expiresAt;
  @Builder.Default private Optional<Instant> endedAt = Optional.empty();
  @Builder.Default private Optional<ImpersonationEndReason> endReason = Optional.empty();
}
