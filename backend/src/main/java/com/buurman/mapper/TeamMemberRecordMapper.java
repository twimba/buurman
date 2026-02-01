package com.buurman.mapper;

import com.buurman.domain.TeamMember;
import com.buurman.jooq.generated.tables.records.TeamMembersRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * MapStruct mapper for converting between JOOQ TeamMembersRecord and TeamMember domain object.
 */
@Mapper(componentModel = "spring")
public interface TeamMemberRecordMapper {

    /**
     * Convert JOOQ TeamMembersRecord to TeamMember domain object.
     */
    @Mapping(target = "invitedAt", expression = "java(toInstant(record.getInvitedAt()))")
    @Mapping(target = "joinedAt", expression = "java(toInstant(record.getJoinedAt()))")
    TeamMember toDomain(TeamMembersRecord record);

    /**
     * Convert TeamMember domain object to JOOQ TeamMembersRecord.
     */
    @Mapping(target = "invitedAt", expression = "java(toLocalDateTime(teamMember.getInvitedAt()))")
    @Mapping(target = "joinedAt", expression = "java(toLocalDateTime(teamMember.getJoinedAt()))")
    TeamMembersRecord toRecord(TeamMember teamMember);

    /**
     * Convert list of JOOQ records to list of domain objects.
     */
    List<TeamMember> toDomainList(List<TeamMembersRecord> records);

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
