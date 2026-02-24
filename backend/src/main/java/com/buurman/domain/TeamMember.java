package com.buurman.domain;

import java.time.Instant;
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
public class TeamMember {

  private UUID id;
  private UUID teamId;
  private UUID userId;
  private TeamRole role;
  private boolean isOwner;
  private Instant invitedAt;
  private UUID invitedBy;
  private Instant joinedAt;

  public TeamMember(
      UUID id,
      UUID teamId,
      UUID userId,
      TeamRole role,
      Instant invitedAt,
      UUID invitedBy,
      Instant joinedAt) {
    this.id = id;
    this.teamId = teamId;
    this.userId = userId;
    this.role = role;
    this.invitedAt = invitedAt;
    this.invitedBy = invitedBy;
    this.joinedAt = joinedAt;
  }
}
