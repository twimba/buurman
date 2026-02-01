package com.buurman.repository.impl;

import com.buurman.domain.Team;
import com.buurman.mapper.TeamRecordMapper;
import com.buurman.repository.TeamRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAMS;

/**
 * JOOQ implementation of TeamRepository.
 */
@Repository
public class TeamRepositoryImpl implements TeamRepository {

    private final DSLContext dsl;
    private final TeamRecordMapper mapper;

    public TeamRepositoryImpl(DSLContext dsl, TeamRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return dsl.selectFrom(TEAMS)
                .where(TEAMS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public Team save(Team team) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (team.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(TEAMS)
                    .set(TEAMS.ID, newId)
                    .set(TEAMS.BUSINESS_ID, team.getBusinessId())
                    .set(TEAMS.NAME, team.getName())
                    .set(TEAMS.CREATED_AT, now)
                    .set(TEAMS.UPDATED_AT, now)
                    .set(TEAMS.CREATED_BY, team.getCreatedBy())
                    .execute();

            team.setId(newId);
            team.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            team.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE
            dsl.update(TEAMS)
                    .set(TEAMS.BUSINESS_ID, team.getBusinessId())
                    .set(TEAMS.NAME, team.getName())
                    .set(TEAMS.UPDATED_AT, now)
                    .set(TEAMS.CREATED_BY, team.getCreatedBy())
                    .where(TEAMS.ID.eq(team.getId()))
                    .execute();

            team.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return team;
    }

    @Override
    public void deleteById(UUID id) {
        dsl.deleteFrom(TEAMS)
                .where(TEAMS.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<Team> findByBusinessId(String businessId) {
        return dsl.selectFrom(TEAMS)
                .where(TEAMS.BUSINESS_ID.eq(businessId))
                .fetchOptional()
                .map(mapper::toDomain);
    }
}
