package com.buurman.mapper;

import com.buurman.domain.TeamMember;
import com.buurman.jooq.generated.tables.records.TeamMembersRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamMemberRecordMapper {

    @Mapping(target = "invitedAt", expression = "java(toInstant(record.getInvitedAt()))")
    @Mapping(target = "joinedAt", expression = "java(toInstant(record.getJoinedAt()))")
    @Mapping(target = "owner", source = "isOwner")
    TeamMember toDomain(TeamMembersRecord record);

    @Mapping(target = "invitedAt", expression = "java(toLocalDateTime(teamMember.getInvitedAt()))")
    @Mapping(target = "joinedAt", expression = "java(toLocalDateTime(teamMember.getJoinedAt()))")
    @Mapping(target = "isOwner", source = "owner")
    TeamMembersRecord toRecord(TeamMember teamMember);

    List<TeamMember> toDomainList(List<TeamMembersRecord> records);

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
