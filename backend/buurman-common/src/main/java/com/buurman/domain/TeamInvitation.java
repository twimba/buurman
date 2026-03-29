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
public class TeamInvitation {

  private UUID id;
  private UUID teamId;
  private String email;
  private TeamRole role;
  private String token;
  private Instant expiresAt;
  private UUID invitedBy;
  private Instant invitedAt;
  @Builder.Default private Optional<Instant> acceptedAt = Optional.empty();
  @Builder.Default private Optional<UUID> acceptedBy = Optional.empty();
  @Builder.Default private Optional<Instant> emailSentAt = Optional.empty();
  @Builder.Default private Optional<String> emailError = Optional.empty();
  @Builder.Default private Optional<String> pendingFirstName = Optional.empty();
  @Builder.Default private Optional<String> pendingLastName = Optional.empty();
  @Builder.Default private Optional<Instant> resentAt = Optional.empty();
  @Builder.Default private Optional<Integer> resentCount = Optional.empty();

  public TeamInvitation(
      UUID id,
      UUID teamId,
      String email,
      TeamRole role,
      String token,
      Instant expiresAt,
      UUID invitedBy,
      Instant invitedAt,
      Optional<Instant> acceptedAt,
      Optional<UUID> acceptedBy) {
    this.id = id;
    this.teamId = teamId;
    this.email = email;
    this.role = role;
    this.token = token;
    this.expiresAt = expiresAt;
    this.invitedBy = invitedBy;
    this.invitedAt = invitedAt;
    this.acceptedAt = acceptedAt;
    this.acceptedBy = acceptedBy;
  }
}
