package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.exception.NotFoundException;

@DisplayName("BackofficePrincipal")
class BackofficePrincipalTest {

  private static final String KEYCLOAK_ID = "kc-backoffice-123";
  private static final String EMAIL = "admin@buurman.io";
  private static final String NAME = "Admin User";

  @Nested
  @DisplayName("construction")
  class Construction {

    @Test
    @DisplayName("stores all fields correctly")
    void storesAllFields() {
      BackofficePrincipal principal =
          new BackofficePrincipal(
              KEYCLOAK_ID, Optional.of(EMAIL), Optional.of(NAME), Optional.of("BACKOFFICE_ADMIN"));

      assertThat(principal.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
      assertThat(principal.getEmail()).contains(EMAIL);
      assertThat(principal.getRole()).contains("BACKOFFICE_ADMIN");
    }

    @Test
    @DisplayName("handles empty optionals")
    void handlesEmptyOptionals() {
      BackofficePrincipal principal =
          new BackofficePrincipal(
              KEYCLOAK_ID, Optional.empty(), Optional.empty(), Optional.empty());

      assertThat(principal.getEmail()).isEmpty();
      assertThat(principal.getRole()).isEmpty();
    }
  }

  @Nested
  @DisplayName("getName (Principal interface)")
  class GetName {

    @Test
    @DisplayName("returns name when present")
    void returnsNameWhenPresent() {
      BackofficePrincipal principal =
          new BackofficePrincipal(
              KEYCLOAK_ID, Optional.of(EMAIL), Optional.of(NAME), Optional.empty());

      assertThat(principal.getName()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("falls back to keycloakId when name is absent")
    void fallsBackToKeycloakId() {
      BackofficePrincipal principal =
          new BackofficePrincipal(
              KEYCLOAK_ID, Optional.of(EMAIL), Optional.empty(), Optional.empty());

      assertThat(principal.getName()).isEqualTo(KEYCLOAK_ID);
    }
  }

  @Nested
  @DisplayName("userId")
  class UserId {

    @Test
    @DisplayName("parses a valid UUID subject")
    void parsesValidUuid() {
      UUID id = UUID.randomUUID();
      BackofficePrincipal principal =
          new BackofficePrincipal(
              id.toString(), Optional.empty(), Optional.empty(), Optional.empty());

      assertThat(principal.userId()).isEqualTo(id);
    }

    @Test
    @DisplayName("throws NotFoundException for a non-UUID subject")
    void throwsForNonUuid() {
      BackofficePrincipal principal =
          new BackofficePrincipal(
              "not-a-uuid", Optional.empty(), Optional.empty(), Optional.empty());

      assertThatThrownBy(principal::userId).isInstanceOf(NotFoundException.class);
    }
  }
}
