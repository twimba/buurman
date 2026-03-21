package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamRole;
import com.buurman.domain.User;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;

@DisplayName("JwtAuthenticationConverter")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationConverterTest {

  private static final Instant NOW = Instant.parse("2026-03-15T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private static final String KEYCLOAK_ID = "kc-sub-123";
  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John Doe";

  private static final UUID USER_ID = UUID.randomUUID();
  private static final Sid USER_SID = Sid.of("USR01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final UUID TEAM_A_ID = UUID.randomUUID();
  private static final UUID TEAM_B_ID = UUID.randomUUID();
  private static final Sid TEAM_A_SID = Sid.of("TEA01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final Sid TEAM_B_SID = Sid.of("TEA02HQJK4B2X5M3N7P8Q9R0S1T2");

  @Mock private UserRepository userRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private TeamRepository teamRepository;

  private JwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() {
    converter = new JwtAuthenticationConverter(userRepository, teamMemberRepository, teamRepository, CLOCK);
  }

  private Jwt buildJwt(Map<String, Object> extraClaims) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(KEYCLOAK_ID)
            .claim("email", EMAIL)
            .claim("name", NAME);
    extraClaims.forEach(builder::claim);
    return builder.build();
  }

  private User buildUser(Optional<UUID> activeTeamId, Optional<UUID> defaultTeamId) {
    return User.builder()
        .id(USER_ID)
        .identifier(Optional.of(USER_SID))
        .keycloakId(KEYCLOAK_ID)
        .email(EMAIL)
        .firstName("John")
        .lastName("Doe")
        .activeTeamId(activeTeamId)
        .defaultTeamId(defaultTeamId)
        .emailVerifiedAt(Optional.of(NOW))
        .build();
  }

  private TeamMember buildMembership(UUID teamId, TeamRole role, boolean isOwner) {
    return TeamMember.builder()
        .id(UUID.randomUUID())
        .teamId(teamId)
        .userId(USER_ID)
        .role(role)
        .isOwner(isOwner)
        .invitedAt(NOW)
        .build();
  }

  private Team buildTeam(UUID teamId, Sid sid) {
    return Team.builder()
        .id(teamId)
        .identifier(Optional.of(sid))
        .name("Test Team")
        .build();
  }

  @Nested
  @DisplayName("user lookup and creation")
  class UserLookupAndCreation {

    @Test
    @DisplayName("finds existing user by keycloakId")
    void findsExistingUser() {
      User existingUser = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      converter.convert(buildJwt(Map.of()));

      verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("creates new user from JWT when keycloakId not found")
    void createsNewUser() {
      User savedUser = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
      when(userRepository.save(any())).thenReturn(savedUser);
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      converter.convert(buildJwt(Map.of()));

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());

      User created = captor.getValue();
      assertThat(created.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
      assertThat(created.getEmail()).isEqualTo(EMAIL);
      assertThat(created.getFirstName()).isEqualTo("John");
      assertThat(created.getLastName()).isEqualTo("Doe");
      assertThat(created.getEmailVerifiedAt()).isPresent();
    }

    @Test
    @DisplayName("splits single-word name into firstName only")
    void splitsSingleWordName() {
      User savedUser = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
      when(userRepository.save(any())).thenReturn(savedUser);
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt =
          Jwt.withTokenValue("token")
              .header("alg", "RS256")
              .subject(KEYCLOAK_ID)
              .claim("email", EMAIL)
              .claim("name", "Madonna")
              .build();

      converter.convert(jwt);

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());
      assertThat(captor.getValue().getFirstName()).isEqualTo("Madonna");
      assertThat(captor.getValue().getLastName()).isEmpty();
    }

    @Test
    @DisplayName("splits multi-word name correctly (first + rest)")
    void splitsMultiWordName() {
      User savedUser = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
      when(userRepository.save(any())).thenReturn(savedUser);
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt =
          Jwt.withTokenValue("token")
              .header("alg", "RS256")
              .subject(KEYCLOAK_ID)
              .claim("email", EMAIL)
              .claim("name", "Jean Claude Van Damme")
              .build();

      converter.convert(jwt);

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());
      assertThat(captor.getValue().getFirstName()).isEqualTo("Jean");
      assertThat(captor.getValue().getLastName()).isEqualTo("Claude Van Damme");
    }
  }

  @Nested
  @DisplayName("team membership selection priority")
  class MembershipSelection {

    @Test
    @DisplayName("priority 1: selects activeTeamId membership")
    void selectsActiveTeam() {
      User user = buildUser(Optional.of(TEAM_A_ID), Optional.of(TEAM_B_ID));
      TeamMember memberA = buildMembership(TEAM_A_ID, TeamRole.TEAM_ADMIN, true);
      TeamMember memberB = buildMembership(TEAM_B_ID, TeamRole.TEAM_VIEWER, false);
      Team teamA = buildTeam(TEAM_A_ID, TEAM_A_SID);

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(memberA, memberB));
      when(teamRepository.findById(TEAM_A_ID)).thenReturn(Optional.of(teamA));

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      UserPrincipal principal = result.getPrincipal();
      assertThat(principal.getTeamId()).contains(TEAM_A_ID);
      assertThat(principal.getRole()).contains(TeamRole.TEAM_ADMIN);
      assertThat(principal.isOwner()).isTrue();
    }

    @Test
    @DisplayName("priority 2: falls back to defaultTeamId when activeTeamId is empty")
    void fallsBackToDefaultTeam() {
      User user = buildUser(Optional.empty(), Optional.of(TEAM_B_ID));
      TeamMember memberA = buildMembership(TEAM_A_ID, TeamRole.TEAM_ADMIN, true);
      TeamMember memberB = buildMembership(TEAM_B_ID, TeamRole.TEAM_VIEWER, false);
      Team teamB = buildTeam(TEAM_B_ID, TEAM_B_SID);

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(memberA, memberB));
      when(teamRepository.findById(TEAM_B_ID)).thenReturn(Optional.of(teamB));

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      UserPrincipal principal = result.getPrincipal();
      assertThat(principal.getTeamId()).contains(TEAM_B_ID);
      assertThat(principal.getRole()).contains(TeamRole.TEAM_VIEWER);
    }

    @Test
    @DisplayName("priority 2: falls back to defaultTeamId when activeTeamId has no matching membership")
    void fallsBackToDefaultWhenActiveTeamNotInMemberships() {
      UUID unknownTeamId = UUID.randomUUID();
      User user = buildUser(Optional.of(unknownTeamId), Optional.of(TEAM_B_ID));
      TeamMember memberB = buildMembership(TEAM_B_ID, TeamRole.TEAM_EDITOR, false);
      Team teamB = buildTeam(TEAM_B_ID, TEAM_B_SID);

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(memberB));
      when(teamRepository.findById(TEAM_B_ID)).thenReturn(Optional.of(teamB));

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getPrincipal().getTeamId()).contains(TEAM_B_ID);
      assertThat(result.getPrincipal().getRole()).contains(TeamRole.TEAM_EDITOR);
    }

    @Test
    @DisplayName("priority 3: falls back to first membership when no active or default team")
    void fallsBackToFirstMembership() {
      User user = buildUser(Optional.empty(), Optional.empty());
      TeamMember memberA = buildMembership(TEAM_A_ID, TeamRole.TEAM_ADMIN, true);
      TeamMember memberB = buildMembership(TEAM_B_ID, TeamRole.TEAM_VIEWER, false);
      Team teamA = buildTeam(TEAM_A_ID, TEAM_A_SID);

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(memberA, memberB));
      when(teamRepository.findById(TEAM_A_ID)).thenReturn(Optional.of(teamA));

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getPrincipal().getTeamId()).contains(TEAM_A_ID);
    }
  }

  @Nested
  @DisplayName("user without team membership")
  class NoTeamMembership {

    @Test
    @DisplayName("creates principal with empty team/role when no memberships exist")
    void noMemberships() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      UserPrincipal principal = result.getPrincipal();
      assertThat(principal.hasNoTeam()).isTrue();
      assertThat(principal.getTeamId()).isEmpty();
      assertThat(principal.getTeamIdentifier()).isEmpty();
      assertThat(principal.getRole()).isEmpty();
      assertThat(principal.isOwner()).isFalse();
    }

    @Test
    @DisplayName("creates principal with empty team when team was deleted after membership")
    void deletedTeam() {
      User user = buildUser(Optional.empty(), Optional.empty());
      TeamMember membership = buildMembership(TEAM_A_ID, TeamRole.TEAM_ADMIN, true);
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(membership));
      when(teamRepository.findById(TEAM_A_ID)).thenReturn(Optional.empty());

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      UserPrincipal principal = result.getPrincipal();
      assertThat(principal.hasNoTeam()).isTrue();
      assertThat(principal.getRole()).isEmpty();
    }
  }

  @Nested
  @DisplayName("email verification")
  class EmailVerification {

    @Test
    @DisplayName("emailVerified is true when emailVerifiedAt is present")
    void emailVerifiedWhenPresent() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getPrincipal().isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("emailVerified is false when emailVerifiedAt is empty")
    void emailNotVerifiedWhenAbsent() {
      User user =
          User.builder()
              .id(USER_ID)
              .identifier(Optional.of(USER_SID))
              .keycloakId(KEYCLOAK_ID)
              .email(EMAIL)
              .firstName("John")
              .lastName("Doe")
              .emailVerifiedAt(Optional.empty())
              .build();

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getPrincipal().isEmailVerified()).isFalse();
    }
  }

  @Nested
  @DisplayName("realm roles extraction")
  class RealmRoles {

    @Test
    @DisplayName("extracts custom realm roles with ROLE_ prefix")
    void extractsCustomRealmRoles() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("BETA_TESTER", "PREMIUM"))));

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder("ROLE_BETA_TESTER", "ROLE_PREMIUM");
    }

    @Test
    @DisplayName("filters out team roles from realm_access (handled via membership)")
    void filtersOutTeamRoles() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt =
          buildJwt(
              Map.of(
                  "realm_access",
                  Map.of("roles", List.of("TEAM_ADMIN", "TEAM_EDITOR", "TEAM_VIEWER", "CUSTOM_ROLE"))));

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_CUSTOM_ROLE");
    }

    @Test
    @DisplayName("filters out default-roles-* prefixed roles")
    void filtersOutDefaultRoles() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt =
          buildJwt(
              Map.of("realm_access", Map.of("roles", List.of("default-roles-buurman", "CUSTOM_ROLE"))));

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_CUSTOM_ROLE");
    }

    @Test
    @DisplayName("returns empty authorities when no realm_access claim")
    void emptyAuthoritiesWhenNoRealmAccess() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("returns empty authorities when realm_access.roles is not a collection")
    void emptyAuthoritiesWhenRolesNotCollection() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", "not_a_list")));

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities()).isEmpty();
    }
  }

  @Nested
  @DisplayName("combined authorities (team role + realm roles)")
  class CombinedAuthorities {

    @Test
    @DisplayName("combines team membership role with realm roles")
    void combinesTeamAndRealmRoles() {
      User user = buildUser(Optional.empty(), Optional.empty());
      TeamMember membership = buildMembership(TEAM_A_ID, TeamRole.TEAM_EDITOR, false);
      Team team = buildTeam(TEAM_A_ID, TEAM_A_SID);

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(membership));
      when(teamRepository.findById(TEAM_A_ID)).thenReturn(Optional.of(team));

      Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("BETA_TESTER"))));

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder("ROLE_TEAM_EDITOR", "ROLE_BETA_TESTER");
    }

    @Test
    @DisplayName("only includes team role when no realm roles present (user with team)")
    void onlyTeamRoleWhenNoRealmRoles() {
      User user = buildUser(Optional.empty(), Optional.empty());
      TeamMember membership = buildMembership(TEAM_A_ID, TeamRole.TEAM_ADMIN, true);
      Team team = buildTeam(TEAM_A_ID, TEAM_A_SID);

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(List.of(membership));
      when(teamRepository.findById(TEAM_A_ID)).thenReturn(Optional.of(team));

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getAuthorities())
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_TEAM_ADMIN");
    }
  }

  @Nested
  @DisplayName("authentication result")
  class AuthenticationResult {

    @Test
    @DisplayName("returns authenticated UserAuthentication")
    void returnsAuthenticatedToken() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      var result = converter.convert(buildJwt(Map.of()));

      assertThat(result).isInstanceOf(UserAuthentication.class);
      assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("principal has correct user identifier from Sid")
    void principalHasUserIdentifier() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      UserAuthentication result = (UserAuthentication) converter.convert(buildJwt(Map.of()));

      assertThat(result.getPrincipal().getUserIdentifier()).isEqualTo(USER_SID.value());
    }

    @Test
    @DisplayName("throws when user identifier is empty")
    void throwsWhenUserIdentifierEmpty() {
      User user =
          User.builder()
              .id(USER_ID)
              .identifier(Optional.empty())
              .keycloakId(KEYCLOAK_ID)
              .email(EMAIL)
              .firstName("John")
              .lastName("Doe")
              .emailVerifiedAt(Optional.of(NOW))
              .build();

      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      assertThatThrownBy(() -> converter.convert(buildJwt(Map.of())))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("User identifier is null");
    }
  }

  @Nested
  @DisplayName("null/empty JWT claims")
  class NullJwtClaims {

    @Test
    @DisplayName("handles null email claim gracefully")
    void handlesNullEmail() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt =
          Jwt.withTokenValue("token")
              .header("alg", "RS256")
              .subject(KEYCLOAK_ID)
              .build();

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      // null email claim → Objects.toString(null, "") → ""
      assertThat(result.getPrincipal().getEmail()).isEmpty();
    }

    @Test
    @DisplayName("handles null name claim gracefully")
    void handlesNullName() {
      User user = buildUser(Optional.empty(), Optional.empty());
      when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(teamMemberRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

      Jwt jwt =
          Jwt.withTokenValue("token")
              .header("alg", "RS256")
              .subject(KEYCLOAK_ID)
              .build();

      UserAuthentication result = (UserAuthentication) converter.convert(jwt);

      assertThat(result.getPrincipal().getName()).isEmpty();
    }
  }
}
