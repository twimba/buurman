package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("BackofficeJwtAuthenticationConverter")
class BackofficeJwtAuthenticationConverterTest {

  private BackofficeJwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() {
    converter = new BackofficeJwtAuthenticationConverter();
  }

  private Jwt buildJwt(
      String keycloakId,
      Optional<String> email,
      Optional<String> name,
      Optional<Map<String, Object>> realmAccess) {
    Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject(keycloakId);
    email.ifPresent(e -> builder.claim("email", e));
    name.ifPresent(n -> builder.claim("name", n));
    realmAccess.ifPresent(ra -> builder.claim("realm_access", ra));
    return builder.build();
  }

  private Jwt buildJwtWithRoles(String keycloakId, Map<String, Object> realmAccess) {
    return buildJwt(
        keycloakId,
        Optional.of("admin@buurman.io"),
        Optional.of("Admin"),
        Optional.of(realmAccess));
  }

  private Jwt buildJwtMinimal(String keycloakId) {
    return buildJwt(
        keycloakId, Optional.of("admin@buurman.io"), Optional.of("Admin"), Optional.empty());
  }

  @Nested
  @DisplayName("principal extraction")
  class PrincipalExtraction {

    @Test
    @DisplayName("extracts keycloakId, email, and name from JWT")
    void extractsFields() {
      Jwt jwt =
          buildJwt(
              "kc-admin-123",
              Optional.of("admin@buurman.io"),
              Optional.of("Admin User"),
              Optional.empty());

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);
      BackofficePrincipal principal = result.getPrincipal();

      assertThat(principal.getKeycloakId()).isEqualTo("kc-admin-123");
      assertThat(principal.getEmail()).contains("admin@buurman.io");
      assertThat(principal.getName()).isEqualTo("Admin User");
    }

    @Test
    @DisplayName("wraps absent email claim as empty Optional")
    void wrapsAbsentEmail() {
      Jwt jwt = buildJwt("kc-admin-123", Optional.empty(), Optional.of("Admin"), Optional.empty());

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getPrincipal().getEmail()).isEmpty();
    }

    @Test
    @DisplayName("wraps absent name claim and getName() falls back to keycloakId")
    void wrapsAbsentName() {
      Jwt jwt =
          buildJwt(
              "kc-admin-123", Optional.of("admin@buurman.io"), Optional.empty(), Optional.empty());

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getPrincipal().getName()).isEqualTo("kc-admin-123");
    }
  }

  @Nested
  @DisplayName("realm roles extraction")
  class RealmRoles {

    @Test
    @DisplayName("extracts realm roles with ROLE_ prefix")
    void extractsRealmRoles() {
      Jwt jwt =
          buildJwtWithRoles(
              "kc-admin-123", Map.of("roles", List.of("BACKOFFICE_ADMIN", "SUPPORT_AGENT")));

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder("ROLE_BACKOFFICE_ADMIN", "ROLE_SUPPORT_AGENT");
    }

    @Test
    @DisplayName("filters out default-roles-* prefixed roles")
    void filtersDefaultRoles() {
      Jwt jwt =
          buildJwtWithRoles(
              "kc-admin-123",
              Map.of("roles", List.of("BACKOFFICE_ADMIN", "default-roles-backoffice")));

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_BACKOFFICE_ADMIN");
    }

    @Test
    @DisplayName("returns empty authorities when no realm_access claim")
    void emptyWhenNoRealmAccess() {
      Jwt jwt = buildJwtMinimal("kc-admin-123");

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("returns empty authorities when roles is not a collection")
    void emptyWhenRolesNotCollection() {
      Jwt jwt = buildJwtWithRoles("kc-admin-123", Map.of("roles", "not_a_list"));

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities()).isEmpty();
    }
  }

  @Nested
  @DisplayName("BACKOFFICE_ADMIN role detection")
  class BackofficeAdminRole {

    @Test
    @DisplayName("sets role to BACKOFFICE_ADMIN when present in realm roles")
    void setsBackofficeAdminRole() {
      Jwt jwt = buildJwtWithRoles("kc-admin-123", Map.of("roles", List.of("BACKOFFICE_ADMIN")));

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getPrincipal().getRole()).contains("BACKOFFICE_ADMIN");
    }

    @Test
    @DisplayName("sets empty role when BACKOFFICE_ADMIN is not in realm roles")
    void emptyRoleWhenNotBackofficeAdmin() {
      Jwt jwt = buildJwtWithRoles("kc-admin-123", Map.of("roles", List.of("SUPPORT_AGENT")));

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getPrincipal().getRole()).isEmpty();
    }

    @Test
    @DisplayName("sets empty role when no realm roles at all")
    void emptyRoleWhenNoRoles() {
      Jwt jwt = buildJwtMinimal("kc-admin-123");

      BackofficeAuthentication result = (BackofficeAuthentication) converter.convert(jwt);

      assertThat(result.getPrincipal().getRole()).isEmpty();
    }
  }

  @Nested
  @DisplayName("authentication result")
  class AuthenticationResult {

    @Test
    @DisplayName("returns authenticated BackofficeAuthentication")
    void returnsAuthenticated() {
      Jwt jwt = buildJwtMinimal("kc-admin-123");

      var result = converter.convert(jwt);

      assertThat(result).isInstanceOf(BackofficeAuthentication.class);
      assertThat(result.isAuthenticated()).isTrue();
    }
  }
}
