package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Team;
import com.buurman.jooq.generated.tables.records.TeamsRecord;

@Component
public class TeamRecordMapper {

  public Optional<Team> toDomain(@Nullable TeamsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    Team team = new Team();
    team.setId(record.getId());
    team.setIdentifier(record.getIdentifier());
    team.setName(record.getName());
    team.setDemo(record.getDemo());
    team.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    team.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    team.setCreatedBy(record.getCreatedBy());
    team.setUpdatedBy(record.getUpdatedBy());
    team.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(team);
  }
}
