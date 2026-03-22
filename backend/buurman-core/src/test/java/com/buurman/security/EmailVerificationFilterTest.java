package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.TeamRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

@DisplayName("EmailVerificationFilter")
@ExtendWith(MockitoExtension.class)
class EmailVerificationFilterTest {

  private static final Instant NOW = Instant.parse("2026-03-15T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private FilterChain filterChain;

  private EmailVerificationFilter filter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    filter = new EmailVerificationFilter(objectMapper, CLOCK);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(boolean emailVerified) {
    UserPrincipal principal =
        new UserPrincipal(
            UUID.randomUUID(),
            "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
            "kc-123",
            "user@example.com",
            "John Doe",
            UUID.randomUUID(),
            "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
            TeamRole.TEAM_ADMIN,
            true,
            emailVerified);
    UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Nested
  @DisplayName("allowed paths (whitelist)")
  class AllowedPaths {

    @ParameterizedTest(name = "allows {0} without verification check")
    @ValueSource(
        strings = {
          "/auth/me",
          "/auth/verify-email",
          "/auth/verify-email-token",
          "/auth/resend-verification"
        })
    @DisplayName("whitelisted path")
    void allowsWhitelistedPaths(String path) throws Exception {
      setAuthenticatedUser(false); // email NOT verified

      MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
      request.setRequestURI(path);
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("unauthenticated requests")
  class UnauthenticatedRequests {

    @Test
    @DisplayName("passes through when no authentication in SecurityContext")
    void passesThroughWhenNoAuth() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      request.setRequestURI("/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("verified email")
  class VerifiedEmail {

    @Test
    @DisplayName("passes through when email is verified")
    void passesThroughWhenVerified() throws Exception {
      setAuthenticatedUser(true);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      request.setRequestURI("/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("unverified email")
  class UnverifiedEmail {

    @Test
    @DisplayName("returns 403 with EMAIL_NOT_VERIFIED error for non-whitelisted paths")
    void returns403WhenNotVerified() throws Exception {
      setAuthenticatedUser(false);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      request.setRequestURI("/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      assertThat(response.getStatus()).isEqualTo(403);
      assertThat(response.getContentType()).isEqualTo("application/json");

      var json = objectMapper.readTree(response.getContentAsString());
      assertThat(json.get("status").asInt()).isEqualTo(403);
      assertThat(json.get("error").asText()).isEqualTo("EMAIL_NOT_VERIFIED");
      assertThat(json.get("message").asText()).contains("verify your email");
      assertThat(json.has("timestamp")).isTrue();
    }

    @Test
    @DisplayName("does not forward request to filter chain")
    void doesNotForward() throws Exception {
      setAuthenticatedUser(false);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      request.setRequestURI("/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verifyNoMoreInteractions(filterChain);
    }
  }
}
