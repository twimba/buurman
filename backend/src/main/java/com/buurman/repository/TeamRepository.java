package com.buurman.repository;

import com.buurman.domain.Team;
import com.buurman.domain.TeamSettings;
import com.buurman.mapper.TeamRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAMS;

@Repository
public class TeamRepository {

    private static final Logger log = LoggerFactory.getLogger(TeamRepository.class);

    private final DSLContext dsl;
    private final TeamRecordMapper mapper;
    private final ObjectMapper objectMapper;

    public TeamRepository(DSLContext dsl, TeamRecordMapper mapper, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public Optional<Team> findById(UUID id) {
        return dsl.selectFrom(TEAMS)
                .where(TEAMS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Team save(Team team) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        // Serialize settings to JSONB
        JSONB settingsJsonb = serializeSettings(team.getSettings());

        if (team.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(TEAMS)
                    .set(TEAMS.ID, newId)
                    .set(TEAMS.IDENTIFIER, team.getIdentifier())
                    .set(TEAMS.NAME, team.getName())
                    .set(TEAMS.SETTINGS, settingsJsonb)
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
                    .set(TEAMS.SETTINGS, settingsJsonb)
                    .set(TEAMS.UPDATED_AT, now)
                    .set(TEAMS.UPDATED_BY, team.getUpdatedBy())
                    .where(TEAMS.ID.eq(team.getId()))
                    .execute();

            team.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return team;
    }

    public List<Team> findAllWithAutoGenerationEnabled() {
        // Query teams where settings->payments->autoGenerationEnabled = true
        return dsl.selectFrom(TEAMS)
                .where("settings->'payments'->>'autoGenerationEnabled' = 'true'")
                .fetch()
                .map(mapper::toDomain);
    }

    private JSONB serializeSettings(TeamSettings settings) {
        if (settings == null) {
            settings = new TeamSettings(); // Use defaults
        }
        try {
            String json = objectMapper.writeValueAsString(settings);
            return JSONB.valueOf(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize team settings", e);
            return JSONB.valueOf("{}");
        }
    }

    public void softDeleteById(UUID id) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(TEAMS)
                .set(TEAMS.DELETED_AT, now)
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
