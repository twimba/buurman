package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.TeamMember;
import com.buurman.jooq.generated.tables.records.TeamMembersRecord;

@Mapper(componentModel = "spring")
public interface TeamMemberRecordMapper {

  @Mapping(target = "invitedAt", expression = "java(toInstant(record.getInvitedAt()))")
  @Mapping(target = "joinedAt", expression = "java(toInstant(record.getJoinedAt()))")
  @Mapping(target = "isOwner", source = "isOwner")
  @Mapping(target = "role", expression = "java(parseRole(record.getRole()))")
  TeamMember toDomain(TeamMembersRecord record);

  @Mapping(target = "invitedAt", expression = "java(toLocalDateTime(teamMember.getInvitedAt()))")
  @Mapping(target = "joinedAt", expression = "java(toLocalDateTime(teamMember.getJoinedAt()))")
  @Mapping(target = "isOwner", source = "owner")
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "role", expression = "java(teamMember.getRole().name())")
  TeamMembersRecord toRecord(TeamMember teamMember);

  List<TeamMember> toDomainList(List<TeamMembersRecord> records);

  default com.buurman.domain.TeamRole parseRole(String role) {
    try {
      return com.buurman.domain.TeamRole.valueOf(role);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Unknown team role: " + role, e);
    }
  }

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
