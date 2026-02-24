package com.buurman.security;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.TeamRole;

public class UserPrincipal implements Principal {
  private final UUID userId;
  private final String userIdentifier; // ULID
  private final String keycloakId;
  private final String email;
  private final String name;
  private final Optional<UUID> teamId; // empty for users without team membership
  private final Optional<String> teamIdentifier; // ULID, empty if no team
  private final Optional<TeamRole> role; // empty for users without team membership
  private final boolean isOwner;
  private final boolean emailVerified;

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

  public UUID getUserId() {
    return userId;
  }

  public String getUserIdentifier() {
    return userIdentifier;
  }

  public String getKeycloakId() {
    return keycloakId;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String getName() {
    return name;
  }

  public Optional<UUID> getTeamId() {
    return teamId;
  }

  public Optional<String> getTeamIdentifier() {
    return teamIdentifier;
  }

  public Optional<TeamRole> getRole() {
    return role;
  }

  public boolean isOwner() {
    return isOwner;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public boolean hasTeam() {
    return teamId.isPresent();
  }

  /** Returns team ID, throwing if the user has no active team membership. */
  public UUID requireTeamId() {
    return teamId.orElseThrow(
        () -> new IllegalStateException("User has no active team membership"));
  }

  /** Returns team identifier (ULID), throwing if the user has no active team membership. */
  public String requireTeamIdentifier() {
    return teamIdentifier.orElseThrow(
        () -> new IllegalStateException("User has no active team membership"));
  }
}
