package com.buurman.security;

import java.security.Principal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public class UserPrincipal implements Principal {
  private final UUID userId;
  private final String userIdentifier; // ULID
  private final String keycloakId;
  private final String email;
  private final String name;
  private final @Nullable UUID teamId; // nullable for users without team membership
  private final @Nullable String teamIdentifier; // ULID, nullable
  private final @Nullable String role; // nullable for users without team membership
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
      @Nullable String role,
      boolean isOwner,
      boolean emailVerified) {
    this.userId = userId;
    this.userIdentifier = userIdentifier;
    this.keycloakId = keycloakId;
    this.email = email;
    this.name = name;
    this.teamId = teamId;
    this.teamIdentifier = teamIdentifier;
    this.role = role;
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
      @Nullable String role,
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
      @Nullable String role) {
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

  public @Nullable UUID getTeamId() {
    return teamId;
  }

  public @Nullable String getTeamIdentifier() {
    return teamIdentifier;
  }

  public @Nullable String getRole() {
    return role;
  }

  public boolean isOwner() {
    return isOwner;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public boolean hasTeam() {
    return teamId != null;
  }
}
