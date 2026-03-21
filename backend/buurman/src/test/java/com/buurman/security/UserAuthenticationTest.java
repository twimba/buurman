package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.buurman.domain.TeamRole;

@DisplayName("UserAuthentication")
class UserAuthenticationTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String USER_IDENTIFIER = "USR01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final String TEAM_IDENTIFIER = "TEA01HQJK4B2X5M3N7P8Q9R0S1T2";

  private UserPrincipal createPrincipal() {
    return new UserPrincipal(
        USER_ID,
        USER_IDENTIFIER,
        "kc-123",
        "user@example.com",
        "John Doe",
        TEAM_ID,
        TEAM_IDENTIFIER,
        TeamRole.TEAM_ADMIN,
        true,
        true);
  }

  @Nested
  @DisplayName("authentication state")
  class AuthenticationState {

    @Test
    @DisplayName("is always authenticated after construction")
    void isAuthenticated() {
      UserAuthentication auth = new UserAuthentication(createPrincipal(), Collections.emptyList());

      assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("returns null credentials (JWT auth, no credentials needed)")
    void credentialsAreNull() {
      UserAuthentication auth = new UserAuthentication(createPrincipal(), Collections.emptyList());

      assertThat(auth.getCredentials()).isNull();
    }
  }

  @Nested
  @DisplayName("principal")
  class Principal {

    @Test
    @DisplayName("returns the UserPrincipal")
    void returnsPrincipal() {
      UserPrincipal principal = createPrincipal();
      UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());

      assertThat(auth.getPrincipal()).isSameAs(principal);
    }
  }

  @Nested
  @DisplayName("authorities")
  class Authorities {

    @Test
    @DisplayName("stores and returns granted authorities")
    void storesAuthorities() {
      List<SimpleGrantedAuthority> authorities =
          List.of(
              new SimpleGrantedAuthority("ROLE_TEAM_ADMIN"),
              new SimpleGrantedAuthority("ROLE_CUSTOM_ROLE"));

      UserAuthentication auth = new UserAuthentication(createPrincipal(), authorities);

      assertThat(auth.getAuthorities()).hasSize(2);
      assertThat(auth.getAuthorities())
          .extracting("authority")
          .containsExactlyInAnyOrder("ROLE_TEAM_ADMIN", "ROLE_CUSTOM_ROLE");
    }

    @Test
    @DisplayName("works with empty authorities")
    void worksWithEmptyAuthorities() {
      UserAuthentication auth = new UserAuthentication(createPrincipal(), Collections.emptyList());

      assertThat(auth.getAuthorities()).isEmpty();
    }
  }
}
