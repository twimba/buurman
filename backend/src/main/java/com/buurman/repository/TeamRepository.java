package com.buurman.repository;

import com.buurman.domain.Team;
import com.buurman.mapper.TeamRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAMS;

@Repository
public class TeamRepository {

    private final DSLContext dsl;
    private final TeamRecordMapper mapper;

    public TeamRepository(DSLContext dsl, TeamRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<Team> findById(UUID id) {
        return dsl.selectFrom(TEAMS)
                .where(TEAMS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Team save(Team team) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (team.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(TEAMS)
                    .set(TEAMS.ID, newId)
                    .set(TEAMS.IDENTIFIER, team.getIdentifier())
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
                    .set(TEAMS.IDENTIFIER, team.getIdentifier())
                    .set(TEAMS.NAME, team.getName())
                    .set(TEAMS.UPDATED_AT, now)
                    .set(TEAMS.CREATED_BY, team.getCreatedBy())
                    .where(TEAMS.ID.eq(team.getId()))
                    .execute();

            team.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return team;
    }

    public void deleteById(UUID id) {
        dsl.deleteFrom(TEAMS)
                .where(TEAMS.ID.eq(id))
                .execute();
    }

    public Optional<Team> findByIdentifier(String identifier) {
        return dsl.selectFrom(TEAMS)
                .where(TEAMS.IDENTIFIER.eq(identifier))
                .fetchOptional()
                .map(mapper::toDomain);
    }
}
