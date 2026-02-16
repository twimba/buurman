package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TeamMember {

  private UUID id;
  private UUID teamId;
  private UUID userId;
  private String role;
  private boolean isOwner;
  private Instant invitedAt;
  private UUID invitedBy;
  private Instant joinedAt;

  public TeamMember(
      UUID id,
      UUID teamId,
      UUID userId,
      String role,
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
