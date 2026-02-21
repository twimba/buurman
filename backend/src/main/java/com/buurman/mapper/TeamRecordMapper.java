package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.buurman.domain.Team;
import com.buurman.jooq.generated.tables.records.TeamsRecord;

@Component
public class TeamRecordMapper {

  public Team toDomain(TeamsRecord record) {
    if (record == null) {
      return null;
    }

    Team team = new Team();
    team.setId(record.getId());
    team.setIdentifier(record.getIdentifier());
    team.setName(record.getName());
    team.setDemo(record.getDemo());
    team.setCreatedAt(toInstant(record.getCreatedAt()));
    team.setUpdatedAt(toInstant(record.getUpdatedAt()));
    team.setCreatedBy(record.getCreatedBy());
    team.setUpdatedBy(record.getUpdatedBy());
    team.setDeletedAt(toInstant(record.getDeletedAt()));

    return team;
  }

  private Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
