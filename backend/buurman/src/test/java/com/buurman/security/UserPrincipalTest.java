package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.TeamRole;

@DisplayName("UserPrincipal")
class UserPrincipalTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String USER_IDENTIFIER = "USR01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final String KEYCLOAK_ID = "kc-abc-123";
  private static final String EMAIL = "user@example.com";
  private static final String NAME = "John Doe";
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final String TEAM_IDENTIFIER = "TEA01HQJK4B2X5M3N7P8Q9R0S1T2";

  @Nested
  @DisplayName("constructor with all parameters")
  class FullConstructor {

    @Test
    @DisplayName("wraps nullable team fields into Optional")
    void wrapsNullableFieldsIntoOptional() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_ADMIN,
              true,
              true);

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
    @DisplayName("accepts null team fields as empty Optionals")
    void acceptsNullTeamFieldsAsEmptyOptionals() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID, USER_IDENTIFIER, KEYCLOAK_ID, EMAIL, NAME, null, null, null, false, false);

      assertThat(principal.getTeamId()).isEmpty();
      assertThat(principal.getTeamIdentifier()).isEmpty();
      assertThat(principal.getRole()).isEmpty();
      assertThat(principal.isOwner()).isFalse();
      assertThat(principal.isEmailVerified()).isFalse();
    }
  }

  @Nested
  @DisplayName("convenience constructors")
  class ConvenienceConstructors {

    @Test
    @DisplayName("two-arg optional constructor defaults emailVerified to true")
    void twoArgOptionalConstructorDefaultsEmailVerified() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_EDITOR,
              true);

      assertThat(principal.isEmailVerified()).isTrue();
      assertThat(principal.isOwner()).isTrue();
      assertThat(principal.getRole()).contains(TeamRole.TEAM_EDITOR);
    }

    @Test
    @DisplayName("three-arg optional constructor defaults isOwner to false and emailVerified to true")
    void threeArgOptionalConstructorDefaultsOwnerAndVerified() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_VIEWER);

      assertThat(principal.isOwner()).isFalse();
      assertThat(principal.isEmailVerified()).isTrue();
      assertThat(principal.getRole()).contains(TeamRole.TEAM_VIEWER);
    }
  }

  @Nested
  @DisplayName("hasNoTeam")
  class HasNoTeam {

    @Test
    @DisplayName("returns true when teamId is empty")
    void returnsTrueWhenNoTeam() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID, USER_IDENTIFIER, KEYCLOAK_ID, EMAIL, NAME, null, null, null, false, true);

      assertThat(principal.hasNoTeam()).isTrue();
    }

    @Test
    @DisplayName("returns false when teamId is present")
    void returnsFalseWhenTeamPresent() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_ADMIN,
              false,
              true);

      assertThat(principal.hasNoTeam()).isFalse();
    }
  }

  @Nested
  @DisplayName("requireTeamId")
  class RequireTeamId {

    @Test
    @DisplayName("returns team ID when present")
    void returnsTeamIdWhenPresent() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_ADMIN);

      assertThat(principal.requireTeamId()).isEqualTo(TEAM_ID);
    }

    @Test
    @DisplayName("throws IllegalStateException when team ID is absent")
    void throwsWhenTeamIdAbsent() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID, USER_IDENTIFIER, KEYCLOAK_ID, EMAIL, NAME, null, null, null, false, true);

      assertThatThrownBy(principal::requireTeamId)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("no active team membership");
    }
  }

  @Nested
  @DisplayName("requireTeamIdentifier")
  class RequireTeamIdentifier {

    @Test
    @DisplayName("returns team identifier when present")
    void returnsTeamIdentifierWhenPresent() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_ADMIN);

      assertThat(principal.requireTeamIdentifier()).isEqualTo(TEAM_IDENTIFIER);
    }

    @Test
    @DisplayName("throws IllegalStateException when team identifier is absent")
    void throwsWhenTeamIdentifierAbsent() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID, USER_IDENTIFIER, KEYCLOAK_ID, EMAIL, NAME, null, null, null, false, true);

      assertThatThrownBy(principal::requireTeamIdentifier)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("no active team membership");
    }
  }

  @Nested
  @DisplayName("getName (Principal interface)")
  class GetName {

    @Test
    @DisplayName("returns name field")
    void returnsName() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID, USER_IDENTIFIER, KEYCLOAK_ID, EMAIL, NAME, null, null, null, false, true);

      assertThat(principal.getName()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("returns empty string when name is empty")
    void returnsEmptyStringWhenNameEmpty() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID, USER_IDENTIFIER, KEYCLOAK_ID, EMAIL, "", null, null, null, false, true);

      assertThat(principal.getName()).isEmpty();
    }
  }

  @Nested
  @DisplayName("all TeamRole values")
  class AllTeamRoles {

    @Test
    @DisplayName("stores TEAM_ADMIN role correctly")
    void storesTeamAdmin() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_ADMIN);

      assertThat(principal.getRole()).contains(TeamRole.TEAM_ADMIN);
    }

    @Test
    @DisplayName("stores TEAM_EDITOR role correctly")
    void storesTeamEditor() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_EDITOR);

      assertThat(principal.getRole()).contains(TeamRole.TEAM_EDITOR);
    }

    @Test
    @DisplayName("stores TEAM_VIEWER role correctly")
    void storesTeamViewer() {
      UserPrincipal principal =
          new UserPrincipal(
              USER_ID,
              USER_IDENTIFIER,
              KEYCLOAK_ID,
              EMAIL,
              NAME,
              TEAM_ID,
              TEAM_IDENTIFIER,
              TeamRole.TEAM_VIEWER);

      assertThat(principal.getRole()).contains(TeamRole.TEAM_VIEWER);
    }
  }
}
