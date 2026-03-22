package com.buurman.security;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.TeamRole;

import lombok.Getter;

public class UserPrincipal implements Principal {
  @Getter private final UUID userId;
  @Getter private final String userIdentifier; // Sid
  @Getter private final String keycloakId;
  @Getter private final String email;
  private final String name;
  @Getter private final Optional<UUID> teamId; // empty for users without team membership
  @Getter private final Optional<String> teamIdentifier; // Sid, empty if no team
  @Getter private final Optional<TeamRole> role; // empty for users without team membership
  @Getter private final boolean isOwner;
  @Getter private final boolean emailVerified;

  public UserPrincipal(
      UUID userId,
      String userIdentifier,
      String keycloakId,
      String email,
      String name,
      @Nullable UUID teamId,
      @Nullable String teamIdentifier,
      @Nullable TeamRole role,
      boolean isOwner,
      boolean emailVerified) {
    this.userId = userId;
    this.userIdentifier = userIdentifier;
    this.keycloakId = keycloakId;
    this.email = email;
    this.name = name;
    this.teamId = Optional.ofNullable(teamId);
    this.teamIdentifier = Optional.ofNullable(teamIdentifier);
    this.role = Optional.ofNullable(role);
    this.isOwner = isOwner;
    this.emailVerified = emailVerified;
  }

  public UserPrincipal(
      UUID userId,
      String userIdentifier,
      String keycloakId,
      String email,
      String name,
      @Nullable UUID teamId,
      @Nullable String teamIdentifier,
      @Nullable TeamRole role,
      boolean isOwner) {
    this(
        userId,
        userIdentifier,
        keycloakId,
        email,
        name,
        teamId,
        teamIdentifier,
        role,
        isOwner,
        true);
  }

  public UserPrincipal(
      UUID userId,
      String userIdentifier,
      String keycloakId,
      String email,
      String name,
      @Nullable UUID teamId,
      @Nullable String teamIdentifier,
      @Nullable TeamRole role) {
    this(
        userId, userIdentifier, keycloakId, email, name, teamId, teamIdentifier, role, false, true);
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean hasNoTeam() {
    return teamId.isEmpty();
  }

  /** Returns team ID, throwing if the user has no active team membership. */
  public UUID requireTeamId() {
    return teamId.orElseThrow(
        () -> new IllegalStateException("User has no active team membership"));
  }

  /** Returns team identifier (Sid), throwing if the user has no active team membership. */
  public String requireTeamIdentifier() {
    return teamIdentifier.orElseThrow(
        () -> new IllegalStateException("User has no active team membership"));
  }
}
