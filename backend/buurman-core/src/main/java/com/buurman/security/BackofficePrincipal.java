package com.buurman.security;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import com.buurman.exception.NotFoundException;

import lombok.Getter;

public class BackofficePrincipal implements Principal {
  @Getter private final String keycloakId;
  @Getter private final Optional<String> email;
  private final Optional<String> name;
  @Getter private final Optional<String> role;

  public BackofficePrincipal(
      String keycloakId, Optional<String> email, Optional<String> name, Optional<String> role) {
    this.keycloakId = keycloakId;
    this.email = email;
    this.name = name;
    this.role = role;
  }

  @Override
  public String getName() {
    return name.orElse(keycloakId);
  }

  /**
   * The Keycloak subject as a UUID — the stable per-Buurmy key for backoffice-owned data (layouts,
   * snoozes). Keycloak subjects are UUIDs; a non-UUID subject is treated as "no user context".
   */
  public UUID userId() {
    try {
      return UUID.fromString(keycloakId);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException("No backoffice user context");
    }
  }
}
