package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.TeamRole;

import jakarta.servlet.FilterChain;

@DisplayName("MdcFilter")
class MdcFilterTest {

  private MdcFilter filter;

  @BeforeEach
  void setUp() {
    filter = new MdcFilter();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Nested
  @DisplayName("MDC population")
  class MdcPopulation {

    @Test
    @DisplayName("sets requestId for every request")
    void setsRequestId() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      AtomicReference<String> capturedRequestId = new AtomicReference<>();
      FilterChain chain = (req, res) -> capturedRequestId.set(MDC.get("requestId"));

      filter.doFilterInternal(request, response, chain);

      assertThat(capturedRequestId.get()).isNotNull();
      // Verify it's a valid UUID
      assertThat(UUID.fromString(capturedRequestId.get())).isNotNull();
    }

    @Test
    @DisplayName("sets userId, teamId, and userEmail when authenticated with team")
    void setsUserContextWithTeam() throws Exception {
      UUID userId = UUID.randomUUID();
      UUID teamId = UUID.randomUUID();
      String email = "user@example.com";

      UserPrincipal principal =
          new UserPrincipal(
              userId,
              "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
              "kc-123",
              email,
              "John Doe",
              teamId,
              "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
              TeamRole.TEAM_ADMIN,
              true,
              true);
      UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      AtomicReference<String> capturedUserId = new AtomicReference<>();
      AtomicReference<String> capturedTeamId = new AtomicReference<>();
      AtomicReference<String> capturedEmail = new AtomicReference<>();
      FilterChain chain =
          (req, res) -> {
            capturedUserId.set(MDC.get("userId"));
            capturedTeamId.set(MDC.get("teamId"));
            capturedEmail.set(MDC.get("userEmail"));
          };

      filter.doFilterInternal(request, response, chain);

      assertThat(capturedUserId.get()).isEqualTo(userId.toString());
      assertThat(capturedTeamId.get()).isEqualTo(teamId.toString());
      assertThat(capturedEmail.get()).isEqualTo(email);
    }

    @Test
    @DisplayName("does not set teamId when user has no team")
    void doesNotSetTeamIdWhenNoTeam() throws Exception {
      UserPrincipal principal =
          new UserPrincipal(
              UUID.randomUUID(),
              "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
              "kc-123",
              "user@example.com",
              "John Doe",
              null,
              null,
              null,
              false,
              true);
      UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);

      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      AtomicReference<String> capturedTeamId = new AtomicReference<>();
      AtomicReference<String> capturedUserId = new AtomicReference<>();
      FilterChain chain =
          (req, res) -> {
            capturedTeamId.set(MDC.get("teamId"));
            capturedUserId.set(MDC.get("userId"));
          };

      filter.doFilterInternal(request, response, chain);

      assertThat(capturedTeamId.get()).isNull();
      assertThat(capturedUserId.get()).isNotNull();
    }

    @Test
    @DisplayName("does not set user MDC fields when unauthenticated")
    void doesNotSetUserFieldsWhenUnauthenticated() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      AtomicReference<String> capturedUserId = new AtomicReference<>();
      AtomicReference<String> capturedEmail = new AtomicReference<>();
      FilterChain chain =
          (req, res) -> {
            capturedUserId.set(MDC.get("userId"));
            capturedEmail.set(MDC.get("userEmail"));
          };

      filter.doFilterInternal(request, response, chain);

      assertThat(capturedUserId.get()).isNull();
      assertThat(capturedEmail.get()).isNull();
    }
  }

  @Nested
  @DisplayName("MDC cleanup")
  class MdcCleanup {

    @Test
    @DisplayName("clears MDC after filter chain completes")
    void clearsMdcAfterChain() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, (req, res) -> {});

      assertThat(MDC.get("requestId")).isNull();
      assertThat(MDC.get("userId")).isNull();
      assertThat(MDC.get("teamId")).isNull();
      assertThat(MDC.get("userEmail")).isNull();
    }

    @Test
    @DisplayName("clears MDC even when filter chain throws exception")
    void clearsMdcOnException() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      try {
        filter.doFilterInternal(
            request,
            response,
            (req, res) -> {
              throw new RuntimeException("boom");
            });
      } catch (RuntimeException e) {
        // expected
      }

      assertThat(MDC.get("requestId")).isNull();
    }
  }
}
