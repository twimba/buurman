package com.buurman.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.ImpersonationProperties;
import com.buurman.domain.ImpersonationEndReason;
import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.ImpersonationSession;
import com.buurman.domain.ImpersonationStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.CreateImpersonationRequest;
import com.buurman.dto.request.ExchangeTokenRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.CreateImpersonationResponse;
import com.buurman.dto.response.ImpersonationExchangeResponse;
import com.buurman.dto.response.ImpersonationSessionInfo;
import com.buurman.dto.response.ImpersonationSessionResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.ForbiddenException;
import com.buurman.exception.NotFoundException;
import com.buurman.exception.ReauthenticationRequiredException;
import com.buurman.repository.ImpersonationSessionRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.buurman.util.SidGenerator;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImpersonationService {

  private static final Logger log = LoggerFactory.getLogger(ImpersonationService.class);
  private static final String IMPERSONATION_ISSUER = "buurman-impersonation";

  private final ImpersonationSessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final ImpersonationProperties properties;
  private final Clock clock;

  @Transactional
  public CreateImpersonationResponse createSession(
      CreateImpersonationRequest request, BackofficePrincipal admin, String ipAddress) {

    // Validate re-authentication
    validateReauth(admin);

    // Resolve target user
    User targetUser = userRepository.getByIdentifierUnscoped(request.userIdentifier());

    // Check if user is disabled
    if (targetUser.getDisabledAt().isPresent()) {
      throw new BadRequestException("Cannot impersonate a disabled user");
    }

    // TODO: Check if target user is a backoffice admin (requires cross-realm lookup).
    // Backoffice admins are in a separate Keycloak realm, and there is no direct way
    // to check backoffice admin status from the user record alone.

    // Resolve target team and membership
    var targetTeam = teamRepository.getByIdentifierForBackoffice(request.teamIdentifier());
    TeamMember membership =
        teamMemberRepository
            .findByUserIdAndTeamId(targetUser.getId(), targetTeam.getId())
            .orElseThrow(
                () -> new BadRequestException("User is not a member of the specified team"));

    // Check for existing active session for this admin
    UUID adminUserId = UUID.fromString(admin.getKeycloakId());
    sessionRepository
        .findActiveByAdminUserId(adminUserId)
        .ifPresent(
            existing -> {
              throw new BadRequestException(
                  "You already have an active impersonation session. End it first.");
            });

    // Determine mode and timeout
    ImpersonationMode mode = request.mode().orElse(ImpersonationMode.FULL);
    Duration timeout =
        request
            .timeout()
            .map(t -> t.compareTo(properties.maxTimeout()) > 0 ? properties.maxTimeout() : t)
            .orElse(properties.defaultTimeout());

    Instant now = clock.instant();

    ImpersonationSession session =
        ImpersonationSession.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(SidGenerator.newImpersonationSessionId()))
            .adminUserId(adminUserId)
            .adminEmail(admin.getEmail().orElse("unknown"))
            .adminName(admin.getName())
            .targetUserId(targetUser.getId())
            .targetTeamId(membership.getTeamId())
            .sessionToken(UUID.randomUUID())
            .mode(mode)
            .reason(request.reason())
            .status(ImpersonationStatus.PENDING)
            .ipAddress(Optional.of(ipAddress))
            .createdAt(now)
            .expiresAt(now.plus(timeout))
            .build();

    sessionRepository.insert(session);

    String redirectUrl =
        properties.appBaseUrl() + "/impersonate?token=" + session.getSessionToken();

    log.info(
        "Impersonation session created: admin={} target_user={} team={} mode={} timeout={}",
        admin.getEmail().orElse("unknown"),
        targetUser.getEmail(),
        request.teamIdentifier(),
        mode,
        timeout);

    return new CreateImpersonationResponse(session.getIdentifier().orElseThrow(), redirectUrl);
  }

  @Transactional
  public ImpersonationExchangeResponse exchangeToken(ExchangeTokenRequest request) {
    ImpersonationSession session =
        sessionRepository
            .findBySessionToken(request.sessionToken())
            .orElseThrow(() -> new NotFoundException("Invalid or expired session token"));

    // Validate not expired
    Instant now = clock.instant();
    if (now.isAfter(session.getExpiresAt())) {
      throw new BadRequestException("Session token has expired");
    }

    // Load target user, team, and membership
    User targetUser = userRepository.getById(session.getTargetUserId());
    Team targetTeam = teamRepository.getById(session.getTargetTeamId());
    TeamMember membership =
        teamMemberRepository
            .findByUserIdAndTeamId(targetUser.getId(), session.getTargetTeamId())
            .orElseThrow(() -> new BadRequestException("User is no longer a member of the team"));

    // Activate session if PENDING, or just invalidate token if already ACTIVE
    if (session.getStatus() == ImpersonationStatus.PENDING) {
      int updated =
          sessionRepository.activate(session.getId(), LocalDateTime.ofInstant(now, ZoneOffset.UTC));
      if (updated == 0) {
        throw new BadRequestException("Session token has already been used");
      }
    } else {
      // ACTIVE session (rejoin): invalidate token so it can't be reused
      sessionRepository.updateSessionToken(session.getId(), UUID.randomUUID());
    }

    // Generate impersonation JWT
    long remainingSeconds = Duration.between(now, session.getExpiresAt()).toSeconds();
    String jwt = generateJwt(session, targetUser, targetTeam, membership);

    log.info(
        "Impersonation session activated: session={} admin={} target={}",
        session.getIdentifier().orElse(Sid.of("unknown")),
        session.getAdminEmail(),
        targetUser.getEmail());

    return new ImpersonationExchangeResponse(
        jwt,
        session.getIdentifier().orElseThrow(),
        session.getAdminEmail(),
        session.getAdminName(),
        session.getMode().name(),
        remainingSeconds,
        targetUser.getEmail(),
        targetTeam.getIdentifier().orElse(Sid.of("unknown")),
        session.getReason());
  }

  @Transactional
  public CreateImpersonationResponse rejoinSession(
      Sid sessionIdentifier, BackofficePrincipal admin) {

    ImpersonationSession session =
        sessionRepository
            .findByIdentifier(sessionIdentifier)
            .orElseThrow(() -> new NotFoundException("Impersonation session not found"));

    // Validate the caller is the same admin
    UUID adminUserId = UUID.fromString(admin.getKeycloakId());
    if (!adminUserId.equals(session.getAdminUserId())) {
      throw new ForbiddenException("Only the admin who created the session can rejoin it");
    }

    // Check session status is PENDING or ACTIVE
    if (session.getStatus() != ImpersonationStatus.PENDING
        && session.getStatus() != ImpersonationStatus.ACTIVE) {
      throw new BadRequestException(
          "Session is no longer active (status: " + session.getStatus() + ")");
    }

    // Check session hasn't expired
    if (session.getExpiresAt().isBefore(clock.instant())) {
      throw new BadRequestException("Session has expired");
    }

    // Generate a new session token
    UUID newToken = UUID.randomUUID();
    sessionRepository.updateSessionToken(session.getId(), newToken);

    String redirectUrl = properties.appBaseUrl() + "/impersonate?token=" + newToken;

    log.info(
        "Impersonation session rejoin: admin={} session={}",
        admin.getEmail().orElse("unknown"),
        sessionIdentifier);

    return new CreateImpersonationResponse(session.getIdentifier().orElseThrow(), redirectUrl);
  }

  @Transactional
  public void endSession(Sid sessionIdentifier, ImpersonationEndReason reason) {
    ImpersonationSession session =
        sessionRepository
            .findByIdentifier(sessionIdentifier)
            .orElseThrow(() -> new NotFoundException("Impersonation session not found"));

    sessionRepository.endSession(session.getId(), reason);

    log.info("Impersonation session ended: session={} reason={}", sessionIdentifier, reason);
  }

  @Transactional
  public void terminateSession(Sid sessionIdentifier) {
    endSession(sessionIdentifier, ImpersonationEndReason.ADMIN_TERMINATED);
  }

  public ImpersonationSessionInfo getSessionInfo(Sid sessionIdentifier) {
    ImpersonationSession session =
        sessionRepository
            .findByIdentifier(sessionIdentifier)
            .orElseThrow(() -> new NotFoundException("Impersonation session not found"));

    if (session.getStatus() != ImpersonationStatus.ACTIVE) {
      throw new BadRequestException("Session is not active");
    }

    Instant now = clock.instant();
    long remaining = Math.max(0, Duration.between(now, session.getExpiresAt()).toSeconds());

    User targetUser = userRepository.getById(session.getTargetUserId());

    return new ImpersonationSessionInfo(
        session.getIdentifier().orElseThrow(),
        session.getAdminEmail(),
        session.getAdminName(),
        session.getMode().name(),
        remaining,
        targetUser.getEmail(),
        session.getReason());
  }

  public PageResponse<ImpersonationSessionResponse> listSessions(PageRequest pageRequest) {
    PaginatedResult<ImpersonationSession> result = sessionRepository.findAllPaginated(pageRequest);

    // Batch-load all target users and teams to avoid N+1
    List<UUID> targetUserIds =
        result.items().stream().map(ImpersonationSession::getTargetUserId).distinct().toList();
    Map<UUID, User> userMap =
        userRepository.findByIds(targetUserIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

    List<UUID> targetTeamIds =
        result.items().stream().map(ImpersonationSession::getTargetTeamId).distinct().toList();
    Map<UUID, Team> teamMap =
        teamRepository.findByIds(targetTeamIds).stream()
            .collect(Collectors.toMap(Team::getId, Function.identity()));

    List<ImpersonationSessionResponse> responses =
        result.items().stream().map(session -> toResponse(session, userMap, teamMap)).toList();

    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional
  public int expireOverdueSessions() {
    return sessionRepository.expireOverdueSessions();
  }

  public ImpersonationSessionResponse getSessionDetail(Sid sessionIdentifier) {
    ImpersonationSession session =
        sessionRepository
            .findByIdentifier(sessionIdentifier)
            .orElseThrow(() -> new NotFoundException("Impersonation session not found"));

    User targetUser = userRepository.findById(session.getTargetUserId()).orElse(null);
    Map<UUID, User> userMap =
        targetUser != null ? Map.of(targetUser.getId(), targetUser) : Map.of();
    Team targetTeam = teamRepository.findById(session.getTargetTeamId()).orElse(null);
    Map<UUID, Team> teamMap =
        targetTeam != null ? Map.of(targetTeam.getId(), targetTeam) : Map.of();
    return toResponse(session, userMap, teamMap);
  }

  private ImpersonationSessionResponse toResponse(
      ImpersonationSession session, Map<UUID, User> userMap, Map<UUID, Team> teamMap) {
    User targetUser = userMap.get(session.getTargetUserId());
    Team targetTeam = teamMap.get(session.getTargetTeamId());

    return new ImpersonationSessionResponse(
        session.getIdentifier().orElseThrow(),
        session.getAdminEmail(),
        session.getAdminName(),
        targetUser != null
            ? targetUser.getIdentifier().orElse(Sid.of("unknown"))
            : Sid.of("unknown"),
        targetUser != null ? targetUser.getEmail() : "unknown",
        targetTeam != null
            ? targetTeam.getIdentifier().orElse(Sid.of("unknown"))
            : Sid.of("unknown"),
        session.getMode().name(),
        session.getReason(),
        session.getStatus().name(),
        session.getCreatedAt(),
        session.getActivatedAt(),
        session.getExpiresAt(),
        session.getEndedAt(),
        session.getEndReason().map(Enum::name));
  }

  private void validateReauth(BackofficePrincipal admin) {
    Instant authTime =
        admin
            .getAuthTime()
            .orElseThrow(
                () ->
                    new ReauthenticationRequiredException(
                        "Re-authentication required to impersonate users"));

    Instant now = clock.instant();
    Duration sinceAuth = Duration.between(authTime, now);

    if (sinceAuth.compareTo(properties.reauthWindow()) > 0) {
      throw new ReauthenticationRequiredException(
          "Re-authentication required. Last authentication was "
              + sinceAuth.toSeconds()
              + " seconds ago (max: "
              + properties.reauthWindow().toSeconds()
              + "s)");
    }
  }

  private String generateJwt(
      ImpersonationSession session, User targetUser, Team targetTeam, TeamMember membership) {
    Instant now = clock.instant();

    return Jwts.builder()
        .issuer(IMPERSONATION_ISSUER)
        .subject(targetUser.getId().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(session.getExpiresAt()))
        .claim("user_id", targetUser.getId().toString())
        .claim("user_identifier", targetUser.getIdentifier().map(Sid::value).orElse(""))
        .claim("keycloak_id", targetUser.getKeycloakId())
        .claim("email", targetUser.getEmail())
        .claim("name", targetUser.getFirstName() + " " + targetUser.getLastName())
        .claim("team_id", membership.getTeamId().toString())
        .claim("team_identifier", targetTeam.getIdentifier().map(Sid::value).orElse(""))
        .claim("role", membership.getRole().name())
        .claim("is_owner", membership.isOwner())
        .claim("impersonation_session_id", session.getIdentifier().map(Sid::value).orElse(""))
        .claim("session_uuid", session.getId().toString())
        .claim("impersonated_by_email", session.getAdminEmail())
        .claim("impersonated_by_name", session.getAdminName())
        .claim("mode", session.getMode().name())
        .signWith(properties.signingKey())
        .compact();
  }
}
