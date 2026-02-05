package com.buurman.repository;

import com.buurman.domain.TeamMember;
import com.buurman.mapper.TeamMemberRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;

@Repository
public class TeamMemberRepository {

    private final DSLContext dsl;
    private final TeamMemberRecordMapper mapper;

    public TeamMemberRepository(DSLContext dsl, TeamMemberRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<TeamMember> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.ID.eq(id)
                        .and(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                        .and(TEAM_MEMBERS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public TeamMember save(TeamMember teamMember) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (teamMember.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(TEAM_MEMBERS)
                    .set(TEAM_MEMBERS.ID, newId)
                    .set(TEAM_MEMBERS.TEAM_ID, teamMember.getTeamId())
                    .set(TEAM_MEMBERS.USER_ID, teamMember.getUserId())
                    .set(TEAM_MEMBERS.ROLE, teamMember.getRole())
                    .set(TEAM_MEMBERS.IS_OWNER, teamMember.isOwner())
                    .set(TEAM_MEMBERS.INVITED_AT, mapper.toLocalDateTime(teamMember.getInvitedAt()))
                    .set(TEAM_MEMBERS.INVITED_BY, teamMember.getInvitedBy())
                    .set(TEAM_MEMBERS.JOINED_AT, mapper.toLocalDateTime(teamMember.getJoinedAt()))
                    .execute();

            teamMember.setId(newId);
        } else {
            // UPDATE
            dsl.update(TEAM_MEMBERS)
                    .set(TEAM_MEMBERS.TEAM_ID, teamMember.getTeamId())
                    .set(TEAM_MEMBERS.USER_ID, teamMember.getUserId())
                    .set(TEAM_MEMBERS.ROLE, teamMember.getRole())
                    .set(TEAM_MEMBERS.IS_OWNER, teamMember.isOwner())
                    .set(TEAM_MEMBERS.INVITED_AT, mapper.toLocalDateTime(teamMember.getInvitedAt()))
                    .set(TEAM_MEMBERS.INVITED_BY, teamMember.getInvitedBy())
                    .set(TEAM_MEMBERS.JOINED_AT, mapper.toLocalDateTime(teamMember.getJoinedAt()))
                    .where(TEAM_MEMBERS.ID.eq(teamMember.getId()))
                    .execute();
        }

        return teamMember;
    }

    public void softDeleteById(UUID id) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(TEAM_MEMBERS)
                .set(TEAM_MEMBERS.DELETED_AT, now)
                .where(TEAM_MEMBERS.ID.eq(id))
                .execute();
    }

    public List<TeamMember> findAllByUserId(UUID userId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.USER_ID.eq(userId)
                        .and(TEAM_MEMBERS.DELETED_AT.isNull()))
                .orderBy(TEAM_MEMBERS.INVITED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Optional<TeamMember> findByUserIdAndTeamId(UUID userId, UUID teamId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.USER_ID.eq(userId)
                        .and(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                        .and(TEAM_MEMBERS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<TeamMember> findOwnerByTeamId(UUID teamId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.TEAM_ID.eq(teamId)
                        .and(TEAM_MEMBERS.IS_OWNER.eq(true))
                        .and(TEAM_MEMBERS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<TeamMember> findByTeamId(UUID teamId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.TEAM_ID.eq(teamId)
                        .and(TEAM_MEMBERS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
    }

    public boolean existsByTeamIdAndUserId(UUID teamId, UUID userId) {
        return dsl.fetchExists(
                dsl.selectFrom(TEAM_MEMBERS)
                        .where(TEAM_MEMBERS.TEAM_ID.eq(teamId)
                                .and(TEAM_MEMBERS.USER_ID.eq(userId))
                                .and(TEAM_MEMBERS.DELETED_AT.isNull()))
        );
    }
}
