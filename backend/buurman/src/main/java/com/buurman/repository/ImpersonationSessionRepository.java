package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.IMPERSONATION_SESSIONS;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ImpersonationEndReason;
import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.ImpersonationSession;
import com.buurman.domain.ImpersonationStatus;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.jooq.generated.tables.records.ImpersonationSessionsRecord;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ImpersonationSessionRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public void insert(ImpersonationSession session) {
    dsl.insertInto(IMPERSONATION_SESSIONS)
        .set(IMPERSONATION_SESSIONS.ID, session.getId())
        .set(IMPERSONATION_SESSIONS.IDENTIFIER, session.getIdentifier().orElseThrow())
        .set(IMPERSONATION_SESSIONS.ADMIN_USER_ID, session.getAdminUserId())
        .set(IMPERSONATION_SESSIONS.ADMIN_EMAIL, session.getAdminEmail())
        .set(IMPERSONATION_SESSIONS.ADMIN_NAME, session.getAdminName())
        .set(IMPERSONATION_SESSIONS.TARGET_USER_ID, session.getTargetUserId())
        .set(IMPERSONATION_SESSIONS.TARGET_TEAM_ID, session.getTargetTeamId())
        .set(IMPERSONATION_SESSIONS.SESSION_TOKEN, session.getSessionToken())
        .set(IMPERSONATION_SESSIONS.MODE, session.getMode().name())
        .set(IMPERSONATION_SESSIONS.REASON, session.getReason())
        .set(IMPERSONATION_SESSIONS.STATUS, session.getStatus().name())
        .set(IMPERSONATION_SESSIONS.IP_ADDRESS, session.getIpAddress().orElse(null))
        .set(IMPERSONATION_SESSIONS.CREATED_AT, toLocalDateTime(session.getCreatedAt()))
        .set(IMPERSONATION_SESSIONS.EXPIRES_AT, toLocalDateTime(session.getExpiresAt()))
        .execute();
  }

  public Optional<ImpersonationSession> findBySessionToken(UUID sessionToken) {
    return dsl.selectFrom(IMPERSONATION_SESSIONS)
        .where(IMPERSONATION_SESSIONS.SESSION_TOKEN.eq(sessionToken))
        .and(
            IMPERSONATION_SESSIONS.STATUS.in(
                ImpersonationStatus.PENDING.name(), ImpersonationStatus.ACTIVE.name()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<ImpersonationSession> findByIdentifier(Sid identifier) {
    return dsl.selectFrom(IMPERSONATION_SESSIONS)
        .where(IMPERSONATION_SESSIONS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<ImpersonationSession> findActiveByAdminUserId(UUID adminUserId) {
    return dsl.selectFrom(IMPERSONATION_SESSIONS)
        .where(IMPERSONATION_SESSIONS.ADMIN_USER_ID.eq(adminUserId))
        .and(
            IMPERSONATION_SESSIONS.STATUS.in(
                ImpersonationStatus.PENDING.name(), ImpersonationStatus.ACTIVE.name()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<ImpersonationSession> findActiveSessionById(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.selectFrom(IMPERSONATION_SESSIONS)
        .where(IMPERSONATION_SESSIONS.ID.eq(id))
        .and(IMPERSONATION_SESSIONS.STATUS.eq(ImpersonationStatus.ACTIVE.name()))
        .and(IMPERSONATION_SESSIONS.ENDED_AT.isNull())
        .and(IMPERSONATION_SESSIONS.EXPIRES_AT.gt(now))
        .fetchOptional()
        .map(this::toDomain);
  }

  /**
   * Atomically activates a session. Only transitions from PENDING to ACTIVE. Returns the number of
   * rows affected (0 if the session was not in PENDING state).
   */
  public int activate(UUID sessionId, LocalDateTime activatedAt) {
    return dsl.update(IMPERSONATION_SESSIONS)
        .set(IMPERSONATION_SESSIONS.STATUS, ImpersonationStatus.ACTIVE.name())
        .set(IMPERSONATION_SESSIONS.ACTIVATED_AT, activatedAt)
        .where(IMPERSONATION_SESSIONS.ID.eq(sessionId))
        .and(IMPERSONATION_SESSIONS.STATUS.eq(ImpersonationStatus.PENDING.name()))
        .execute();
  }

  public void endSession(UUID sessionId, ImpersonationEndReason reason) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(IMPERSONATION_SESSIONS)
        .set(IMPERSONATION_SESSIONS.STATUS, ImpersonationStatus.ENDED.name())
        .set(IMPERSONATION_SESSIONS.ENDED_AT, now)
        .set(IMPERSONATION_SESSIONS.END_REASON, reason.name())
        .where(IMPERSONATION_SESSIONS.ID.eq(sessionId))
        .and(
            IMPERSONATION_SESSIONS.STATUS.in(
                ImpersonationStatus.PENDING.name(), ImpersonationStatus.ACTIVE.name()))
        .execute();
  }

  public void updateSessionToken(UUID sessionId, UUID newToken) {
    dsl.update(IMPERSONATION_SESSIONS)
        .set(IMPERSONATION_SESSIONS.SESSION_TOKEN, newToken)
        .where(IMPERSONATION_SESSIONS.ID.eq(sessionId))
        .execute();
  }

  public int expireOverdueSessions() {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.update(IMPERSONATION_SESSIONS)
        .set(IMPERSONATION_SESSIONS.STATUS, ImpersonationStatus.EXPIRED.name())
        .set(IMPERSONATION_SESSIONS.ENDED_AT, now)
        .set(IMPERSONATION_SESSIONS.END_REASON, ImpersonationEndReason.EXPIRED.name())
        .where(
            IMPERSONATION_SESSIONS.STATUS.in(
                ImpersonationStatus.PENDING.name(), ImpersonationStatus.ACTIVE.name()))
        .and(IMPERSONATION_SESSIONS.EXPIRES_AT.lt(now))
        .execute();
  }

  public PaginatedResult<ImpersonationSession> findAllPaginated(PageRequest pageRequest) {
    Condition condition = DSL.trueCondition();
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", IMPERSONATION_SESSIONS.CREATED_AT,
            "status", IMPERSONATION_SESSIONS.STATUS,
            "adminEmail", IMPERSONATION_SESSIONS.ADMIN_EMAIL,
            "expiresAt", IMPERSONATION_SESSIONS.EXPIRES_AT);
    return PaginationHelper.paginate(
        dsl,
        IMPERSONATION_SESSIONS,
        condition,
        sortableFields,
        IMPERSONATION_SESSIONS.CREATED_AT,
        pageRequest,
        this::toDomain);
  }

  public List<UUID> collectTargetUserIds(List<ImpersonationSession> sessions) {
    return sessions.stream().map(ImpersonationSession::getTargetUserId).distinct().toList();
  }

  private ImpersonationSession toDomain(ImpersonationSessionsRecord r) {
    return ImpersonationSession.builder()
        .id(r.getId())
        .identifier(Optional.of(r.getIdentifier()))
        .adminUserId(r.getAdminUserId())
        .adminEmail(r.getAdminEmail())
        .adminName(r.getAdminName())
        .targetUserId(r.getTargetUserId())
        .targetTeamId(r.getTargetTeamId())
        .sessionToken(r.getSessionToken())
        .mode(ImpersonationMode.valueOf(r.getMode()))
        .reason(r.getReason())
        .status(ImpersonationStatus.valueOf(r.getStatus()))
        .ipAddress(Optional.ofNullable(r.getIpAddress()))
        .createdAt(java.util.Objects.requireNonNull(toInstant(r.getCreatedAt())))
        .activatedAt(Optional.ofNullable(toInstant(r.getActivatedAt())))
        .expiresAt(java.util.Objects.requireNonNull(toInstant(r.getExpiresAt())))
        .endedAt(Optional.ofNullable(toInstant(r.getEndedAt())))
        .endReason(Optional.ofNullable(r.getEndReason()).map(ImpersonationEndReason::valueOf))
        .build();
  }

  private static LocalDateTime toLocalDateTime(java.time.Instant instant) {
    return LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
  }

  private static java.time.@org.jspecify.annotations.Nullable Instant toInstant(
      @org.jspecify.annotations.Nullable LocalDateTime ldt) {
    if (ldt == null) {
      return null;
    }
    return ldt.toInstant(java.time.ZoneOffset.UTC);
  }
}
