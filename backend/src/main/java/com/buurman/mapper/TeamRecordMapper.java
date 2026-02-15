package com.buurman.mapper;

import com.buurman.domain.Team;
import com.buurman.domain.TeamSettings;
import com.buurman.jooq.generated.tables.records.TeamsRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;

@Component
@Slf4j
@RequiredArgsConstructor
public class TeamRecordMapper {

    private final ObjectMapper objectMapper;

    public Team toDomain(TeamsRecord record) {
        if (record == null) {
            return null;
        }

        Team team = new Team();
        team.setId(record.getId());
        team.setIdentifier(record.getIdentifier());
        team.setName(record.getName());
        team.setSettings(deserializeSettings(record.getSettings()));
        team.setCreatedAt(toInstant(record.getCreatedAt()));
        team.setUpdatedAt(toInstant(record.getUpdatedAt()));
        team.setCreatedBy(record.getCreatedBy());
        team.setUpdatedBy(record.getUpdatedBy());
        team.setDeletedAt(toInstant(record.getDeletedAt()));

        return team;
    }

    private TeamSettings deserializeSettings(JSONB settingsJsonb) {
        if (settingsJsonb == null || settingsJsonb.data() == null || settingsJsonb.data().isEmpty()) {
            return new TeamSettings(); // Return defaults
        }

        try {
            return objectMapper.readValue(settingsJsonb.data(), TeamSettings.class);
        } catch (Exception e) {
            log.error("Failed to deserialize team settings", e);
            return new TeamSettings(); // Return defaults on error
        }
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(UTC);
    }
}
