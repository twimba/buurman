package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;

@DisplayName("ImpersonationPrincipal")
class ImpersonationPrincipalTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String USER_IDENTIFIER = "USR01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final String KEYCLOAK_ID = "kc-user-456";
  private static final String EMAIL = "tenant@example.com";
  private static final String NAME = "Jane Tenant";
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final String TEAM_IDENTIFIER = "TEA01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final Sid SESSION_ID = Sid.of("IMP01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final UUID SESSION_UUID = UUID.randomUUID();
  private static final String ADMIN_EMAIL = "admin@buurman.io";
  private static final String ADMIN_NAME = "Admin User";

  private ImpersonationPrincipal createPrincipal(ImpersonationMode mode) {
    return new ImpersonationPrincipal(
        USER_ID,
        USER_IDENTIFIER,
        KEYCLOAK_ID,
        EMAIL,
        NAME,
        TEAM_ID,
        TEAM_IDENTIFIER,
        TeamRole.TEAM_ADMIN,
        true,
        true,
        SESSION_ID,
        SESSION_UUID,
        ADMIN_EMAIL,
        ADMIN_NAME,
        mode);
  }

  @Nested
  @DisplayName("construction and field access")
  class Construction {

    @Test
    @DisplayName("stores all impersonation-specific fields")
    void storesImpersonationFields() {
      ImpersonationPrincipal principal = createPrincipal(ImpersonationMode.FULL);

      assertThat(principal.getImpersonationSessionId()).isEqualTo(SESSION_ID);
      assertThat(principal.getImpersonationSessionUuid()).isEqualTo(SESSION_UUID);
      assertThat(principal.getImpersonatedByEmail()).isEqualTo(ADMIN_EMAIL);
      assertThat(principal.getImpersonatedByName()).isEqualTo(ADMIN_NAME);
      assertThat(principal.getMode()).isEqualTo(ImpersonationMode.FULL);
    }

    @Test
    @DisplayName("inherits UserPrincipal fields correctly")
    void inheritsUserPrincipalFields() {
      ImpersonationPrincipal principal = createPrincipal(ImpersonationMode.READ_ONLY);

      assertThat(principal.getUserId()).isEqualTo(USER_ID);
      assertThat(principal.getUserIdentifier()).isEqualTo(USER_IDENTIFIER);
      assertThat(principal.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
      assertThat(principal.getEmail()).isEqualTo(EMAIL);
      assertThat(principal.getName()).isEqualTo(NAME);
      assertThat(principal.getTeamId()).contains(TEAM_ID);
      assertThat(principal.getTeamIdentifier()).contains(TEAM_IDENTIFIER);
      assertThat(principal.getRole()).contains(TeamRole.TEAM_ADMIN);
      assertThat(principal.isOwner()).isTrue();
      assertThat(principal.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("is an instance of UserPrincipal (polymorphism)")
    void isInstanceOfUserPrincipal() {
      ImpersonationPrincipal principal = createPrincipal(ImpersonationMode.FULL);

      assertThat(principal).isInstanceOf(UserPrincipal.class);
    }
  }

  @Nested
  @DisplayName("isReadOnly")
  class IsReadOnly {

    @Test
    @DisplayName("returns true for READ_ONLY mode")
    void trueForReadOnly() {
      ImpersonationPrincipal principal = createPrincipal(ImpersonationMode.READ_ONLY);

      assertThat(principal.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("returns false for FULL mode")
    void falseForFullMode() {
      ImpersonationPrincipal principal = createPrincipal(ImpersonationMode.FULL);

      assertThat(principal.isReadOnly()).isFalse();
    }
  }

  @Nested
  @DisplayName("getAuditDisplayName")
  class GetAuditDisplayName {

    @Test
    @DisplayName("formats as 'AdminName on behalf of UserName'")
    void formatsAuditDisplayName() {
      ImpersonationPrincipal principal = createPrincipal(ImpersonationMode.FULL);

      assertThat(principal.getAuditDisplayName())
          .isEqualTo("Admin User on behalf of Jane Tenant");
    }
  }
}
