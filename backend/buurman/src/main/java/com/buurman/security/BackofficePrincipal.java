package com.buurman.security;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

public class BackofficePrincipal implements Principal {
  @Getter private final String keycloakId;
  @Getter private final Optional<String> email;
  private final Optional<String> name;
  @Getter private final Optional<String> role;
  @Getter private final Optional<Instant> authTime;

  public BackofficePrincipal(
      String keycloakId,
      @Nullable String email,
      @Nullable String name,
      @Nullable String role,
      @Nullable Instant authTime) {
    this.keycloakId = keycloakId;
    this.email = Optional.ofNullable(email);
    this.name = Optional.ofNullable(name);
    this.role = Optional.ofNullable(role);
    this.authTime = Optional.ofNullable(authTime);
  }

  public BackofficePrincipal(
      String keycloakId, @Nullable String email, @Nullable String name, @Nullable String role) {
    this(keycloakId, email, name, role, null);
  }

  @Override
  public String getName() {
    return name.orElse(keycloakId);
  }
}
