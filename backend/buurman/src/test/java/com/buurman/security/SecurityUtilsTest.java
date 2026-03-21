package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;

@DisplayName("SecurityUtils")
class SecurityUtilsTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private UserPrincipal createUserPrincipal() {
    return new UserPrincipal(
        UUID.randomUUID(),
        "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
        "kc-123",
        "user@example.com",
        "John Doe",
        UUID.randomUUID(),
        "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
        TeamRole.TEAM_ADMIN,
        true,
        true);
  }

  private ImpersonationPrincipal createImpersonationPrincipal() {
    return new ImpersonationPrincipal(
        UUID.randomUUID(),
        "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
        "kc-123",
        "user@example.com",
        "John Doe",
        UUID.randomUUID(),
        "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
        TeamRole.TEAM_ADMIN,
        true,
        true,
        Sid.of("IMP01HQJK4B2X5M3N7P8Q9R0S1T2"),
        UUID.randomUUID(),
        "admin@buurman.io",
        "Admin User",
        ImpersonationMode.FULL);
  }

  private BackofficePrincipal createBackofficePrincipal() {
    return new BackofficePrincipal(
        "kc-admin-123",
        Optional.of("admin@buurman.io"),
        Optional.of("Admin"),
        Optional.of("BACKOFFICE_ADMIN"));
  }

  @Nested
  @DisplayName("getCurrentPrincipal")
  class GetCurrentPrincipal {

    @Test
    @DisplayName("returns UserPrincipal when authenticated")
    void returnsUserPrincipal() {
      UserPrincipal principal = createUserPrincipal();
      UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThat(SecurityUtils.getCurrentPrincipal()).isSameAs(principal);
    }

    @Test
    @DisplayName("throws when SecurityContext has no authentication")
    void throwsWhenNoAuth() {
      assertThatThrownBy(SecurityUtils::getCurrentPrincipal)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No authenticated UserPrincipal");
    }

    @Test
    @DisplayName("throws when principal is not UserPrincipal")
    void throwsWhenWrongPrincipalType() {
      BackofficePrincipal backoffice = createBackofficePrincipal();
      BackofficeAuthentication auth =
          new BackofficeAuthentication(
              backoffice, List.of(new SimpleGrantedAuthority("ROLE_BACKOFFICE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThatThrownBy(SecurityUtils::getCurrentPrincipal)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No authenticated UserPrincipal");
    }

    @Test
    @DisplayName("returns ImpersonationPrincipal via UserPrincipal (polymorphism)")
    void returnsImpersonationPrincipalAsUserPrincipal() {
      ImpersonationPrincipal principal = createImpersonationPrincipal();
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      UserPrincipal result = SecurityUtils.getCurrentPrincipal();
      assertThat(result).isSameAs(principal);
      assertThat(result).isInstanceOf(ImpersonationPrincipal.class);
    }
  }

  @Nested
  @DisplayName("getBackofficePrincipal")
  class GetBackofficePrincipal {

    @Test
    @DisplayName("returns BackofficePrincipal when authenticated")
    void returnsBackofficePrincipal() {
      BackofficePrincipal principal = createBackofficePrincipal();
      BackofficeAuthentication auth =
          new BackofficeAuthentication(principal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThat(SecurityUtils.getBackofficePrincipal()).isSameAs(principal);
    }

    @Test
    @DisplayName("throws when SecurityContext has no authentication")
    void throwsWhenNoAuth() {
      assertThatThrownBy(SecurityUtils::getBackofficePrincipal)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No authenticated BackofficePrincipal");
    }

    @Test
    @DisplayName("throws when principal is UserPrincipal (not BackofficePrincipal)")
    void throwsWhenWrongPrincipalType() {
      UserPrincipal userPrincipal = createUserPrincipal();
      UserAuthentication auth = new UserAuthentication(userPrincipal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThatThrownBy(SecurityUtils::getBackofficePrincipal)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No authenticated BackofficePrincipal");
    }
  }

  @Nested
  @DisplayName("isImpersonating")
  class IsImpersonating {

    @Test
    @DisplayName("returns true when ImpersonationPrincipal is in SecurityContext")
    void trueForImpersonation() {
      ImpersonationPrincipal principal = createImpersonationPrincipal();
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThat(SecurityUtils.isImpersonating()).isTrue();
    }

    @Test
    @DisplayName("returns false when regular UserPrincipal is in SecurityContext")
    void falseForRegularUser() {
      UserPrincipal principal = createUserPrincipal();
      UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThat(SecurityUtils.isImpersonating()).isFalse();
    }

    @Test
    @DisplayName("returns false when SecurityContext has no authentication")
    void falseWhenNoAuth() {
      assertThat(SecurityUtils.isImpersonating()).isFalse();
    }
  }

  @Nested
  @DisplayName("getImpersonationPrincipal")
  class GetImpersonationPrincipal {

    @Test
    @DisplayName("returns ImpersonationPrincipal when present")
    void returnsImpersonationPrincipal() {
      ImpersonationPrincipal principal = createImpersonationPrincipal();
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThat(SecurityUtils.getImpersonationPrincipal()).isSameAs(principal);
    }

    @Test
    @DisplayName("returns null when regular UserPrincipal is present")
    void returnsNullForRegularUser() {
      UserPrincipal principal = createUserPrincipal();
      UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      assertThat(SecurityUtils.getImpersonationPrincipal()).isNull();
    }

    @Test
    @DisplayName("returns null when no authentication")
    void returnsNullWhenNoAuth() {
      assertThat(SecurityUtils.getImpersonationPrincipal()).isNull();
    }
  }
}
