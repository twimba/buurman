package com.buurman.mapper;

import com.buurman.domain.Team;
import com.buurman.jooq.generated.tables.records.TeamsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * MapStruct mapper for converting between JOOQ TeamsRecord and Team domain object.
 */
@Mapper(componentModel = "spring")
public interface TeamRecordMapper {

    /**
     * Convert JOOQ TeamsRecord to Team domain object.
     */
    @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
    Team toDomain(TeamsRecord record);

    /**
     * Convert Team domain object to JOOQ TeamsRecord.
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(team.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(team.getUpdatedAt()))")
    TeamsRecord toRecord(Team team);

    /**
     * Convert list of JOOQ records to list of domain objects.
     */
    List<Team> toDomainList(List<TeamsRecord> records);

    /**
     * Convert LocalDateTime to Instant.
     */
    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Convert Instant to LocalDateTime.
     */
    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
