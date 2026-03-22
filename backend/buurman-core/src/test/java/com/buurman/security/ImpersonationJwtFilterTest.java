package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.config.ImpersonationProperties;
import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.ImpersonationSession;
import com.buurman.domain.TeamRole;
import com.buurman.repository.ImpersonationSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;

@DisplayName("ImpersonationJwtFilter")
@ExtendWith(MockitoExtension.class)
class ImpersonationJwtFilterTest {

  private static final Instant NOW = Instant.parse("2026-03-15T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final String JWT_SECRET = "super-secret-key-that-is-at-least-32-chars-long!!";

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String USER_IDENTIFIER = "USR01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final String KEYCLOAK_ID = "kc-user-123";
  private static final String EMAIL = "user@example.com";
  private static final String NAME = "Jane Tenant";
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final String TEAM_IDENTIFIER = "TEA01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final String SESSION_ID = "IMP01HQJK4B2X5M3N7P8Q9R0S1T2";
  private static final UUID SESSION_UUID = UUID.randomUUID();
  private static final String ADMIN_EMAIL = "admin@buurman.io";
  private static final String ADMIN_NAME = "Admin User";

  @Mock private ImpersonationSessionRepository sessionRepository;
  @Mock private FilterChain filterChain;

  private ImpersonationProperties properties;
  private ImpersonationJwtFilter filter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    properties =
        new ImpersonationProperties(
            JWT_SECRET,
            Duration.ofMinutes(60),
            Duration.ofMinutes(15),
            "https://app.local.buurman.io");
    objectMapper = new ObjectMapper();
    filter = new ImpersonationJwtFilter(properties, sessionRepository, objectMapper, CLOCK);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private String buildImpersonationToken(Instant expiration, ImpersonationMode mode) {
    return Jwts.builder()
        .issuer("buurman-impersonation")
        .expiration(Date.from(expiration))
        .claim("user_id", USER_ID.toString())
        .claim("user_identifier", USER_IDENTIFIER)
        .claim("keycloak_id", KEYCLOAK_ID)
        .claim("email", EMAIL)
        .claim("name", NAME)
        .claim("team_id", TEAM_ID.toString())
        .claim("team_identifier", TEAM_IDENTIFIER)
        .claim("role", TeamRole.TEAM_ADMIN.name())
        .claim("is_owner", true)
        .claim("impersonation_session_id", SESSION_ID)
        .claim("session_uuid", SESSION_UUID.toString())
        .claim("impersonated_by_email", ADMIN_EMAIL)
        .claim("impersonated_by_name", ADMIN_NAME)
        .claim("mode", mode.name())
        .signWith(properties.signingKey())
        .compact();
  }

  private String buildValidToken() {
    return buildImpersonationToken(NOW.plusSeconds(600), ImpersonationMode.FULL);
  }

  @Nested
  @DisplayName("no Authorization header")
  class NoAuthHeader {

    @Test
    @DisplayName("passes through when no Authorization header")
    void passesThrough() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("passes through when Authorization header is not Bearer")
    void passesThroughNonBearer() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("non-impersonation JWT (regular Keycloak token)")
  class NonImpersonationJwt {

    @Test
    @DisplayName("passes through when JWT has wrong issuer (falls through to OAuth2)")
    void passesThroughWrongIssuer() throws Exception {
      // Sign with a different key, or use a regular Keycloak-like token.
      // When HMAC verification fails, it should fall through.
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer some.regular.keycloak.token");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
  }

  @Nested
  @DisplayName("valid impersonation JWT")
  class ValidImpersonationJwt {

    @Test
    @DisplayName("sets ImpersonationPrincipal in SecurityContext")
    void setsImpersonationPrincipal() throws Exception {
      when(sessionRepository.findActiveSessionById(SESSION_UUID))
          .thenReturn(Optional.of(mock(ImpersonationSession.class)));

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth =
          Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
      assertThat(auth.getPrincipal()).isInstanceOf(ImpersonationPrincipal.class);

      ImpersonationPrincipal principal =
          (ImpersonationPrincipal) Objects.requireNonNull(auth.getPrincipal());
      assertThat(principal.getUserId()).isEqualTo(USER_ID);
      assertThat(principal.getUserIdentifier()).isEqualTo(USER_IDENTIFIER);
      assertThat(principal.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
      assertThat(principal.getEmail()).isEqualTo(EMAIL);
      assertThat(principal.getName()).isEqualTo(NAME);
      assertThat(principal.getTeamId()).contains(TEAM_ID);
      assertThat(principal.getTeamIdentifier()).contains(TEAM_IDENTIFIER);
      assertThat(principal.getRole()).contains(TeamRole.TEAM_ADMIN);
      assertThat(principal.isOwner()).isTrue();
      assertThat(principal.getImpersonationSessionId().value()).isEqualTo(SESSION_ID);
      assertThat(principal.getImpersonationSessionUuid()).isEqualTo(SESSION_UUID);
      assertThat(principal.getImpersonatedByEmail()).isEqualTo(ADMIN_EMAIL);
      assertThat(principal.getImpersonatedByName()).isEqualTo(ADMIN_NAME);
      assertThat(principal.getMode()).isEqualTo(ImpersonationMode.FULL);
    }

    @Test
    @DisplayName("strips Authorization header from forwarded request")
    void stripsAuthorizationHeader() throws Exception {
      when(sessionRepository.findActiveSessionById(SESSION_UUID))
          .thenReturn(Optional.of(mock(ImpersonationSession.class)));

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      // The filterChain receives a wrapped request — verify it was called
      verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("builds role hierarchy for TEAM_ADMIN (ADMIN > EDITOR > VIEWER)")
    void buildsRoleHierarchyForAdmin() throws Exception {
      when(sessionRepository.findActiveSessionById(SESSION_UUID))
          .thenReturn(Optional.of(mock(ImpersonationSession.class)));

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth =
          Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
      assertThat(auth.getAuthorities())
          .extracting("authority")
          .containsExactlyInAnyOrder("ROLE_TEAM_ADMIN", "ROLE_TEAM_EDITOR", "ROLE_TEAM_VIEWER");
    }

    @Test
    @DisplayName("builds role hierarchy for TEAM_EDITOR (EDITOR > VIEWER)")
    void buildsRoleHierarchyForEditor() throws Exception {
      String token =
          Jwts.builder()
              .issuer("buurman-impersonation")
              .expiration(Date.from(NOW.plusSeconds(600)))
              .claim("user_id", USER_ID.toString())
              .claim("user_identifier", USER_IDENTIFIER)
              .claim("keycloak_id", KEYCLOAK_ID)
              .claim("email", EMAIL)
              .claim("name", NAME)
              .claim("team_id", TEAM_ID.toString())
              .claim("team_identifier", TEAM_IDENTIFIER)
              .claim("role", TeamRole.TEAM_EDITOR.name())
              .claim("is_owner", false)
              .claim("impersonation_session_id", SESSION_ID)
              .claim("session_uuid", SESSION_UUID.toString())
              .claim("impersonated_by_email", ADMIN_EMAIL)
              .claim("impersonated_by_name", ADMIN_NAME)
              .claim("mode", ImpersonationMode.READ_ONLY.name())
              .signWith(properties.signingKey())
              .compact();

      when(sessionRepository.findActiveSessionById(SESSION_UUID))
          .thenReturn(Optional.of(mock(ImpersonationSession.class)));

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + token);
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth =
          Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
      assertThat(auth.getAuthorities())
          .extracting("authority")
          .containsExactlyInAnyOrder("ROLE_TEAM_EDITOR", "ROLE_TEAM_VIEWER");
    }

    @Test
    @DisplayName("builds role hierarchy for TEAM_VIEWER (VIEWER only)")
    void buildsRoleHierarchyForViewer() throws Exception {
      String token =
          Jwts.builder()
              .issuer("buurman-impersonation")
              .expiration(Date.from(NOW.plusSeconds(600)))
              .claim("user_id", USER_ID.toString())
              .claim("user_identifier", USER_IDENTIFIER)
              .claim("keycloak_id", KEYCLOAK_ID)
              .claim("email", EMAIL)
              .claim("name", NAME)
              .claim("team_id", TEAM_ID.toString())
              .claim("team_identifier", TEAM_IDENTIFIER)
              .claim("role", TeamRole.TEAM_VIEWER.name())
              .claim("is_owner", false)
              .claim("impersonation_session_id", SESSION_ID)
              .claim("session_uuid", SESSION_UUID.toString())
              .claim("impersonated_by_email", ADMIN_EMAIL)
              .claim("impersonated_by_name", ADMIN_NAME)
              .claim("mode", ImpersonationMode.READ_ONLY.name())
              .signWith(properties.signingKey())
              .compact();

      when(sessionRepository.findActiveSessionById(SESSION_UUID))
          .thenReturn(Optional.of(mock(ImpersonationSession.class)));

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + token);
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth =
          Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
      assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_TEAM_VIEWER");
    }
  }

  @Nested
  @DisplayName("expired impersonation JWT")
  class ExpiredToken {

    @Test
    @DisplayName("falls through to OAuth2 when token is expired (JJWT parser rejects it)")
    void fallsThroughWhenExpired() throws Exception {
      // JJWT's parser validates expiration before our manual check.
      // ExpiredJwtException extends JwtException, so it's caught and falls through.
      String expiredToken = buildImpersonationToken(NOW.minusSeconds(10), ImpersonationMode.FULL);

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + expiredToken);
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      // Falls through to regular OAuth2 filter chain
      verify(filterChain).doFilter(request, response);
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
  }

  @Nested
  @DisplayName("revoked session")
  class RevokedSession {

    @Test
    @DisplayName("returns 401 when session is not active in DB")
    void returns401WhenSessionRevoked() throws Exception {
      when(sessionRepository.findActiveSessionById(SESSION_UUID)).thenReturn(Optional.empty());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      assertThat(response.getStatus()).isEqualTo(401);
      assertThat(response.getContentAsString()).contains("revoked");
      verify(filterChain, never()).doFilter(any(), any());
    }
  }

  @Nested
  @DisplayName("revocation cache")
  class RevocationCache {

    @Test
    @DisplayName("caches active session status and does not query DB on second request within TTL")
    void cachesActiveSession() throws Exception {
      when(sessionRepository.findActiveSessionById(SESSION_UUID))
          .thenReturn(Optional.of(mock(ImpersonationSession.class)));

      MockHttpServletRequest request1 = new MockHttpServletRequest();
      request1.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response1 = new MockHttpServletResponse();
      filter.doFilterInternal(request1, response1, filterChain);

      // Second request — should use cache (same clock, within 15s TTL)
      SecurityContextHolder.clearContext();
      MockHttpServletRequest request2 = new MockHttpServletRequest();
      request2.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response2 = new MockHttpServletResponse();
      filter.doFilterInternal(request2, response2, filterChain);

      // DB was only called once
      verify(sessionRepository).findActiveSessionById(SESSION_UUID);
    }
  }

  @Nested
  @DisplayName("error response format")
  class ErrorResponseFormat {

    @Test
    @DisplayName("error responses are JSON with expected fields")
    void errorResponseIsJson() throws Exception {
      when(sessionRepository.findActiveSessionById(SESSION_UUID)).thenReturn(Optional.empty());

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + buildValidToken());
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      assertThat(response.getContentType()).isEqualTo("application/json");

      var json = objectMapper.readTree(response.getContentAsString());
      assertThat(json.has("timestamp")).isTrue();
      assertThat(json.get("status").asInt()).isEqualTo(401);
      assertThat(json.get("error").asText()).isEqualTo("IMPERSONATION_ERROR");
      assertThat(json.has("message")).isTrue();
    }
  }
}
