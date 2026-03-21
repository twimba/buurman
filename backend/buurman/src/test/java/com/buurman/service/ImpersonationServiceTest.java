package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.ImpersonationProperties;
import com.buurman.config.models.KeycloakProperties;
import com.buurman.domain.ImpersonationEndReason;
import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.ImpersonationSession;
import com.buurman.domain.ImpersonationStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamRole;
import com.buurman.domain.User;
import com.buurman.dto.request.ExchangeTokenRequest;
import com.buurman.dto.response.ImpersonationExchangeResponse;
import com.buurman.dto.response.ImpersonationSessionInfo;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ImpersonationSessionRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImpersonationService")
class ImpersonationServiceTest {

  @Mock private ImpersonationSessionRepository sessionRepository;
  @Mock private UserRepository userRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private ImpersonationProperties properties;
  @Mock private KeycloakProperties keycloakProperties;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private ImpersonationService service;

  private static final Sid SESSION_SID = Sid.of("imp_01JTEST000000000000000001");

  @BeforeEach
  void setUp() {
    service =
        new ImpersonationService(
            sessionRepository,
            userRepository,
            teamRepository,
            teamMemberRepository,
            properties,
            keycloakProperties,
            clock);
  }

  @Nested
  @DisplayName("endSession")
  class EndSession {

    @Test
    @DisplayName("ends an existing session with given reason")
    void endsExistingSession() {
      ImpersonationSession session =
          ImpersonationSession.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(SESSION_SID))
              .status(ImpersonationStatus.ACTIVE)
              .build();

      when(sessionRepository.findByIdentifier(SESSION_SID)).thenReturn(Optional.of(session));

      service.endSession(SESSION_SID, ImpersonationEndReason.ADMIN_TERMINATED);

      verify(sessionRepository).endSession(session.getId(), ImpersonationEndReason.ADMIN_TERMINATED);
    }

    @Test
    @DisplayName("throws NotFoundException for unknown session")
    void throwsForUnknownSession() {
      when(sessionRepository.findByIdentifier(SESSION_SID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.endSession(SESSION_SID, ImpersonationEndReason.ADMIN_TERMINATED))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("session not found");
    }
  }

  @Nested
  @DisplayName("terminateSession")
  class TerminateSession {

    @Test
    @DisplayName("delegates to endSession with ADMIN_TERMINATED reason")
    void delegatesToEndSession() {
      ImpersonationSession session =
          ImpersonationSession.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(SESSION_SID))
              .status(ImpersonationStatus.ACTIVE)
              .build();

      when(sessionRepository.findByIdentifier(SESSION_SID)).thenReturn(Optional.of(session));

      service.terminateSession(SESSION_SID);

      verify(sessionRepository)
          .endSession(session.getId(), ImpersonationEndReason.ADMIN_TERMINATED);
    }
  }

  @Nested
  @DisplayName("getSessionInfo")
  class GetSessionInfo {

    @Test
    @DisplayName("returns session info with correct fields for active session")
    void returnsSessionInfoForActiveSession() {
      UUID targetUserId = UUID.randomUUID();
      Instant expiresAt = Instant.parse("2026-03-01T13:00:00Z"); // 1 hour from fixed clock

      ImpersonationSession session =
          ImpersonationSession.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(SESSION_SID))
              .adminEmail("admin@example.com")
              .adminName("Admin User")
              .targetUserId(targetUserId)
              .targetTeamId(UUID.randomUUID())
              .mode(ImpersonationMode.FULL)
              .reason("Support ticket #123")
              .status(ImpersonationStatus.ACTIVE)
              .expiresAt(expiresAt)
              .build();

      User targetUser =
          User.builder()
              .id(targetUserId)
              .email("target@example.com")
              .firstName("Target")
              .lastName("User")
              .build();

      when(sessionRepository.findByIdentifier(SESSION_SID)).thenReturn(Optional.of(session));
      when(userRepository.getById(targetUserId)).thenReturn(targetUser);

      ImpersonationSessionInfo info = service.getSessionInfo(SESSION_SID);

      assertThat(info.sessionIdentifier()).isEqualTo(SESSION_SID);
      assertThat(info.adminEmail()).isEqualTo("admin@example.com");
      assertThat(info.adminName()).isEqualTo("Admin User");
      assertThat(info.mode()).isEqualTo("FULL");
      assertThat(info.reason()).isEqualTo("Support ticket #123");
      assertThat(info.targetUserEmail()).isEqualTo("target@example.com");
      assertThat(info.remainingSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("throws BadRequestException when session is not active")
    void throwsWhenSessionNotActive() {
      ImpersonationSession session =
          ImpersonationSession.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(SESSION_SID))
              .status(ImpersonationStatus.ENDED)
              .build();

      when(sessionRepository.findByIdentifier(SESSION_SID)).thenReturn(Optional.of(session));

      assertThatThrownBy(() -> service.getSessionInfo(SESSION_SID))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("throws NotFoundException for unknown session")
    void throwsForUnknownSession() {
      when(sessionRepository.findByIdentifier(SESSION_SID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getSessionInfo(SESSION_SID))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("exchangeToken")
  class ExchangeToken {

    @Test
    @DisplayName("throws BadRequestException when session token has expired")
    void throwsWhenExpired() {
      UUID sessionToken = UUID.randomUUID();
      ImpersonationSession session =
          ImpersonationSession.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(SESSION_SID))
              .sessionToken(sessionToken)
              .status(ImpersonationStatus.PENDING)
              .expiresAt(Instant.parse("2026-02-28T00:00:00Z")) // before our fixed clock
              .build();

      when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));

      ExchangeTokenRequest request = new ExchangeTokenRequest(sessionToken);

      assertThatThrownBy(() -> service.exchangeToken(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("activates PENDING session and returns exchange response")
    void activatesPendingSession() {
      UUID sessionId = UUID.randomUUID();
      UUID sessionToken = UUID.randomUUID();
      UUID targetUserId = UUID.randomUUID();
      UUID targetTeamId = UUID.randomUUID();
      Instant expiresAt = Instant.parse("2026-03-01T13:00:00Z");

      ImpersonationSession session =
          ImpersonationSession.builder()
              .id(sessionId)
              .identifier(Optional.of(SESSION_SID))
              .adminUserId(UUID.randomUUID())
              .adminEmail("admin@example.com")
              .adminName("Admin User")
              .targetUserId(targetUserId)
              .targetTeamId(targetTeamId)
              .sessionToken(sessionToken)
              .mode(ImpersonationMode.FULL)
              .reason("Testing")
              .status(ImpersonationStatus.PENDING)
              .expiresAt(expiresAt)
              .build();

      User targetUser =
          User.builder()
              .id(targetUserId)
              .identifier(Optional.of(Sid.of("usr_01JTEST000000000000000001")))
              .keycloakId("kc-target")
              .email("target@example.com")
              .firstName("Target")
              .lastName("User")
              .build();

      Team targetTeam =
          Team.builder()
              .id(targetTeamId)
              .identifier(Optional.of(Sid.of("team_01JTEST000000000000000001")))
              .name("Test Team")
              .build();

      TeamMember membership =
          TeamMember.builder()
              .id(UUID.randomUUID())
              .teamId(targetTeamId)
              .userId(targetUserId)
              .role(TeamRole.TEAM_ADMIN)
              .isOwner(true)
              .build();

      // Stub a real signing key for JWT generation
      String secret = "a-very-long-secret-key-that-is-at-least-32-chars!!";
      when(properties.signingKey())
          .thenReturn(
              new javax.crypto.spec.SecretKeySpec(
                  secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));

      when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
      when(sessionRepository.activate(eq(sessionId), any())).thenReturn(1);
      when(userRepository.getById(targetUserId)).thenReturn(targetUser);
      when(teamRepository.getById(targetTeamId)).thenReturn(targetTeam);
      when(teamMemberRepository.findByUserIdAndTeamId(targetUserId, targetTeamId))
          .thenReturn(Optional.of(membership));

      ExchangeTokenRequest request = new ExchangeTokenRequest(sessionToken);

      ImpersonationExchangeResponse response = service.exchangeToken(request);

      assertThat(response.token()).isNotBlank();
      assertThat(response.sessionIdentifier()).isEqualTo(SESSION_SID);
      assertThat(response.adminEmail()).isEqualTo("admin@example.com");
      assertThat(response.mode()).isEqualTo("FULL");
      assertThat(response.targetUserEmail()).isEqualTo("target@example.com");
      assertThat(response.expiresIn()).isEqualTo(3600L);
      verify(sessionRepository).activate(eq(sessionId), any());
    }
  }

  @Nested
  @DisplayName("expireOverdueSessions")
  class ExpireOverdue {

    @Test
    @DisplayName("delegates to repository")
    void delegatesToRepository() {
      when(sessionRepository.expireOverdueSessions()).thenReturn(3);

      int result = service.expireOverdueSessions();

      assertThat(result).isEqualTo(3);
      verify(sessionRepository).expireOverdueSessions();
    }
  }
}
