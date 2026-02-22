package com.buurman.security;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

@Getter
public class BackofficePrincipal implements Principal {
  private final String keycloakId;
  private final @Nullable String email;
  private final @Nullable String name;
  private final @Nullable String role;

  public BackofficePrincipal(
      String keycloakId, @Nullable String email, @Nullable String name, @Nullable String role) {
    this.keycloakId = keycloakId;
    this.email = email;
    this.name = name;
    this.role = role;
  }

  @Override
  public @Nullable String getName() {
    return name;
  }
}
