package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TEAM_INVITATIONS;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.TeamInvitation;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.TeamInvitationRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TeamInvitationRepository {

  private final DSLContext dsl;
  private final TeamInvitationRecordMapper mapper;
  private final Clock clock;

  public TeamInvitation save(TeamInvitation invitation) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (invitation.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      dsl.insertInto(TEAM_INVITATIONS)
          .set(TEAM_INVITATIONS.ID, newId)
          .set(TEAM_INVITATIONS.TEAM_ID, invitation.getTeamId())
          .set(TEAM_INVITATIONS.EMAIL, invitation.getEmail())
          .set(TEAM_INVITATIONS.ROLE, invitation.getRole().name())
          .set(TEAM_INVITATIONS.TOKEN, invitation.getToken())
          .set(TEAM_INVITATIONS.EXPIRES_AT, mapper.toLocalDateTime(invitation.getExpiresAt()))
          .set(TEAM_INVITATIONS.INVITED_BY, invitation.getInvitedBy())
          .set(TEAM_INVITATIONS.INVITED_AT, mapper.toLocalDateTime(invitation.getInvitedAt()))
          .set(
              TEAM_INVITATIONS.ACCEPTED_AT,
              mapper.toLocalDateTime(invitation.getAcceptedAt().orElse(null)))
          .set(TEAM_INVITATIONS.ACCEPTED_BY, invitation.getAcceptedBy().orElse(null))
          .set(
              TEAM_INVITATIONS.EMAIL_SENT_AT,
              mapper.toLocalDateTime(invitation.getEmailSentAt().orElse(null)))
          .set(TEAM_INVITATIONS.EMAIL_ERROR, invitation.getEmailError().orElse(null))
          .set(TEAM_INVITATIONS.PENDING_FIRST_NAME, invitation.getPendingFirstName().orElse(null))
          .set(TEAM_INVITATIONS.PENDING_LAST_NAME, invitation.getPendingLastName().orElse(null))
          .set(
              TEAM_INVITATIONS.RESENT_AT,
              mapper.toLocalDateTime(invitation.getResentAt().orElse(null)))
          .set(TEAM_INVITATIONS.RESENT_COUNT, invitation.getResentCount().orElse(null))
          .execute();

      invitation.setId(newId);
    } else {
      // UPDATE
      dsl.update(TEAM_INVITATIONS)
          .set(TEAM_INVITATIONS.TEAM_ID, invitation.getTeamId())
          .set(TEAM_INVITATIONS.EMAIL, invitation.getEmail())
          .set(TEAM_INVITATIONS.ROLE, invitation.getRole().name())
          .set(TEAM_INVITATIONS.TOKEN, invitation.getToken())
          .set(TEAM_INVITATIONS.EXPIRES_AT, mapper.toLocalDateTime(invitation.getExpiresAt()))
          .set(TEAM_INVITATIONS.INVITED_BY, invitation.getInvitedBy())
          .set(TEAM_INVITATIONS.INVITED_AT, mapper.toLocalDateTime(invitation.getInvitedAt()))
          .set(
              TEAM_INVITATIONS.ACCEPTED_AT,
              mapper.toLocalDateTime(invitation.getAcceptedAt().orElse(null)))
          .set(TEAM_INVITATIONS.ACCEPTED_BY, invitation.getAcceptedBy().orElse(null))
          .set(
              TEAM_INVITATIONS.EMAIL_SENT_AT,
              mapper.toLocalDateTime(invitation.getEmailSentAt().orElse(null)))
          .set(TEAM_INVITATIONS.EMAIL_ERROR, invitation.getEmailError().orElse(null))
          .set(TEAM_INVITATIONS.PENDING_FIRST_NAME, invitation.getPendingFirstName().orElse(null))
          .set(TEAM_INVITATIONS.PENDING_LAST_NAME, invitation.getPendingLastName().orElse(null))
          .set(
              TEAM_INVITATIONS.RESENT_AT,
              mapper.toLocalDateTime(invitation.getResentAt().orElse(null)))
          .set(TEAM_INVITATIONS.RESENT_COUNT, invitation.getResentCount().orElse(null))
          .where(TEAM_INVITATIONS.ID.eq(invitation.getId()))
          .execute();
    }

    return invitation;
  }

  public void softDeleteById(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TEAM_INVITATIONS)
        .set(TEAM_INVITATIONS.DELETED_AT, now)
        .where(TEAM_INVITATIONS.ID.eq(id))
        .execute();
  }

  public Optional<TeamInvitation> findByToken(String token) {
    return dsl.selectFrom(TEAM_INVITATIONS)
        .where(TEAM_INVITATIONS.TOKEN.eq(token))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public TeamInvitation getByToken(String token) {
    return findByToken(token).orElseThrow(() -> new NotFoundException("Invitation not found"));
  }

  public List<TeamInvitation> findByTeamIdAndAcceptedAtIsNull(UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(TEAM_INVITATIONS)
            .where(TEAM_INVITATIONS.TEAM_ID.eq(teamId))
            .and(TEAM_INVITATIONS.ACCEPTED_AT.isNull())
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<TeamInvitation> findPendingByEmailAndTeamId(String email, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.selectFrom(TEAM_INVITATIONS)
        .where(TEAM_INVITATIONS.EMAIL.equalIgnoreCase(email))
        .and(TEAM_INVITATIONS.TEAM_ID.eq(teamId))
        .and(TEAM_INVITATIONS.ACCEPTED_AT.isNull())
        .and(TEAM_INVITATIONS.EXPIRES_AT.greaterThan(now))
        .and(TEAM_INVITATIONS.DELETED_AT.isNull())
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public List<TeamInvitation> findPendingByTeamId(UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return List.copyOf(
        dsl.selectFrom(TEAM_INVITATIONS)
            .where(TEAM_INVITATIONS.TEAM_ID.eq(teamId))
            .and(TEAM_INVITATIONS.ACCEPTED_AT.isNull())
            .and(TEAM_INVITATIONS.EXPIRES_AT.greaterThan(now))
            .and(TEAM_INVITATIONS.DELETED_AT.isNull())
            .orderBy(TEAM_INVITATIONS.INVITED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<TeamInvitation> findPendingByEmail(String email) {
    LocalDateTime now = LocalDateTime.now(clock);
    return List.copyOf(
        dsl.selectFrom(TEAM_INVITATIONS)
            .where(TEAM_INVITATIONS.EMAIL.equalIgnoreCase(email))
            .and(TEAM_INVITATIONS.ACCEPTED_AT.isNull())
            .and(TEAM_INVITATIONS.EXPIRES_AT.greaterThan(now))
            .and(TEAM_INVITATIONS.DELETED_AT.isNull())
            .orderBy(TEAM_INVITATIONS.INVITED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }
}
