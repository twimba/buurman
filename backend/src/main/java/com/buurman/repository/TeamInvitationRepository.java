package com.buurman.repository;

import com.buurman.domain.TeamInvitation;
import com.buurman.mapper.TeamInvitationRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAM_INVITATIONS;

@Repository
public class TeamInvitationRepository {

    private final DSLContext dsl;
    private final TeamInvitationRecordMapper mapper;

    public TeamInvitationRepository(DSLContext dsl, TeamInvitationRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<TeamInvitation> findById(UUID id) {
        return dsl.selectFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public TeamInvitation save(TeamInvitation invitation) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (invitation.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(TEAM_INVITATIONS)
                    .set(TEAM_INVITATIONS.ID, newId)
                    .set(TEAM_INVITATIONS.TEAM_ID, invitation.getTeamId())
                    .set(TEAM_INVITATIONS.EMAIL, invitation.getEmail())
                    .set(TEAM_INVITATIONS.ROLE, invitation.getRole())
                    .set(TEAM_INVITATIONS.TOKEN, invitation.getToken())
                    .set(TEAM_INVITATIONS.EXPIRES_AT, mapper.toLocalDateTime(invitation.getExpiresAt()))
                    .set(TEAM_INVITATIONS.INVITED_BY, invitation.getInvitedBy())
                    .set(TEAM_INVITATIONS.INVITED_AT, mapper.toLocalDateTime(invitation.getInvitedAt()))
                    .set(TEAM_INVITATIONS.ACCEPTED_AT, mapper.toLocalDateTime(invitation.getAcceptedAt()))
                    .set(TEAM_INVITATIONS.ACCEPTED_BY, invitation.getAcceptedBy())
                    .execute();

            invitation.setId(newId);
        } else {
            // UPDATE
            dsl.update(TEAM_INVITATIONS)
                    .set(TEAM_INVITATIONS.TEAM_ID, invitation.getTeamId())
                    .set(TEAM_INVITATIONS.EMAIL, invitation.getEmail())
                    .set(TEAM_INVITATIONS.ROLE, invitation.getRole())
                    .set(TEAM_INVITATIONS.TOKEN, invitation.getToken())
                    .set(TEAM_INVITATIONS.EXPIRES_AT, mapper.toLocalDateTime(invitation.getExpiresAt()))
                    .set(TEAM_INVITATIONS.INVITED_BY, invitation.getInvitedBy())
                    .set(TEAM_INVITATIONS.INVITED_AT, mapper.toLocalDateTime(invitation.getInvitedAt()))
                    .set(TEAM_INVITATIONS.ACCEPTED_AT, mapper.toLocalDateTime(invitation.getAcceptedAt()))
                    .set(TEAM_INVITATIONS.ACCEPTED_BY, invitation.getAcceptedBy())
                    .where(TEAM_INVITATIONS.ID.eq(invitation.getId()))
                    .execute();
        }

        return invitation;
    }

    public void deleteById(UUID id) {
        dsl.deleteFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.ID.eq(id))
                .execute();
    }

    public Optional<TeamInvitation> findByToken(String token) {
        return dsl.selectFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.TOKEN.eq(token))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<TeamInvitation> findByTeamIdAndAcceptedAtIsNull(UUID teamId) {
        return dsl.selectFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.TEAM_ID.eq(teamId))
                .and(TEAM_INVITATIONS.ACCEPTED_AT.isNull())
                .fetch()
                .map(mapper::toDomain);
    }
}
