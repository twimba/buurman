package com.buurman.security;

import java.security.Principal;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

public class BackofficePrincipal implements Principal {
  private final String keycloakId;
  private final Optional<String> email;
  private final Optional<String> name;
  private final Optional<String> role;

  public BackofficePrincipal(
      String keycloakId, @Nullable String email, @Nullable String name, @Nullable String role) {
    this.keycloakId = keycloakId;
    this.email = Optional.ofNullable(email);
    this.name = Optional.ofNullable(name);
    this.role = Optional.ofNullable(role);
  }

  public String getKeycloakId() {
    return keycloakId;
  }

  public Optional<String> getEmail() {
    return email;
  }

  public Optional<String> getRole() {
    return role;
  }

  @Override
  public @Nullable String getName() {
    return name.orElse(null);
  }
}
