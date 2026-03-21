package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.buurman.domain.ImpersonationSession;
import com.buurman.domain.ImpersonationStatus;
import com.buurman.domain.Sid;
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
  @DisplayName("exchangeToken — expired session")
  class ExchangeTokenExpired {

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

      com.buurman.dto.request.ExchangeTokenRequest request =
          new com.buurman.dto.request.ExchangeTokenRequest(sessionToken);

      assertThatThrownBy(() -> service.exchangeToken(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("expired");
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
