package com.buurman.mapper;

import com.buurman.domain.Team;
import com.buurman.jooq.generated.tables.records.TeamsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamRecordMapper {

    @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
    Team toDomain(TeamsRecord record);

    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(team.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(team.getUpdatedAt()))")
    TeamsRecord toRecord(Team team);

    List<Team> toDomainList(List<TeamsRecord> records);

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
