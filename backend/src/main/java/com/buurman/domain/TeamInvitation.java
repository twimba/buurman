package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class TeamInvitation {

  private UUID id;
  private UUID teamId;
  private String email;
  private String role;
  private String token;
  private Instant expiresAt;
  private UUID invitedBy;
  private Instant invitedAt;
  private @Nullable Instant acceptedAt;
  private @Nullable UUID acceptedBy;
  private @Nullable Instant emailSentAt;
  private @Nullable String emailError;
  private @Nullable String pendingFirstName;
  private @Nullable String pendingLastName;
  private @Nullable Instant resentAt;
  private @Nullable Integer resentCount;

  public TeamInvitation(
      UUID id,
      UUID teamId,
      String email,
      String role,
      String token,
      Instant expiresAt,
      UUID invitedBy,
      Instant invitedAt,
      Instant acceptedAt,
      UUID acceptedBy) {
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
