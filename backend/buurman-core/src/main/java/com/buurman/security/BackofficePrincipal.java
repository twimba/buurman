package com.buurman.security;

import java.security.Principal;
import java.util.Optional;

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
}
