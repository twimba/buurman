package com.buurman.repository.impl;

import com.buurman.domain.TeamMember;
import com.buurman.mapper.TeamMemberRecordMapper;
import com.buurman.repository.TeamMemberRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;

/**
 * JOOQ implementation of TeamMemberRepository.
 */
@Repository
public class TeamMemberRepositoryImpl implements TeamMemberRepository {

    private final DSLContext dsl;
    private final TeamMemberRecordMapper mapper;

    public TeamMemberRepositoryImpl(DSLContext dsl, TeamMemberRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    @Override
    public Optional<TeamMember> findById(UUID id) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
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
                    .set(TEAM_MEMBERS.INVITED_AT, mapper.toLocalDateTime(teamMember.getInvitedAt()))
                    .set(TEAM_MEMBERS.INVITED_BY, teamMember.getInvitedBy())
                    .set(TEAM_MEMBERS.JOINED_AT, mapper.toLocalDateTime(teamMember.getJoinedAt()))
                    .where(TEAM_MEMBERS.ID.eq(teamMember.getId()))
                    .execute();
        }

        return teamMember;
    }

    @Override
    public void deleteById(UUID id) {
        dsl.deleteFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<TeamMember> findByUserId(UUID userId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.USER_ID.eq(userId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public List<TeamMember> findByTeamId(UUID teamId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByTeamIdAndUserId(UUID teamId, UUID userId) {
        return dsl.fetchExists(
                dsl.selectFrom(TEAM_MEMBERS)
                        .where(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                        .and(TEAM_MEMBERS.USER_ID.eq(userId))
        );
    }
}
