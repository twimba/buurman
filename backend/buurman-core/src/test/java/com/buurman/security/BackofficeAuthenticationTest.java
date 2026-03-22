package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@DisplayName("BackofficeAuthentication")
class BackofficeAuthenticationTest {

  private BackofficePrincipal createPrincipal() {
    return new BackofficePrincipal(
        "kc-admin-123",
        Optional.of("admin@buurman.io"),
        Optional.of("Admin"),
        Optional.of("BACKOFFICE_ADMIN"));
  }

  @Test
  @DisplayName("is always authenticated after construction")
  void isAuthenticated() {
    BackofficeAuthentication auth =
        new BackofficeAuthentication(createPrincipal(), Collections.emptyList());

    assertThat(auth.isAuthenticated()).isTrue();
  }

  @Test
  @DisplayName("returns null credentials")
  void credentialsAreNull() {
    BackofficeAuthentication auth =
        new BackofficeAuthentication(createPrincipal(), Collections.emptyList());

    assertThat(auth.getCredentials()).isNull();
  }

  @Test
  @DisplayName("returns BackofficePrincipal")
  void returnsPrincipal() {
    BackofficePrincipal principal = createPrincipal();
    BackofficeAuthentication auth =
        new BackofficeAuthentication(principal, Collections.emptyList());

    assertThat(auth.getPrincipal()).isSameAs(principal);
    assertThat(auth.getPrincipal()).isInstanceOf(BackofficePrincipal.class);
  }

  @Test
  @DisplayName("stores granted authorities")
  void storesAuthorities() {
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_BACKOFFICE_ADMIN"));

    BackofficeAuthentication auth = new BackofficeAuthentication(createPrincipal(), authorities);

    assertThat(auth.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_BACKOFFICE_ADMIN");
  }
}
