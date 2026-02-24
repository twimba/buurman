package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.TeamInvitation;
import com.buurman.jooq.generated.tables.records.TeamInvitationsRecord;

@Mapper(componentModel = "spring")
public interface TeamInvitationRecordMapper {

  @Mapping(target = "expiresAt", expression = "java(toInstant(record.getExpiresAt()))")
  @Mapping(target = "invitedAt", expression = "java(toInstant(record.getInvitedAt()))")
  @Mapping(
      target = "acceptedAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getAcceptedAt())))")
  @Mapping(
      target = "emailSentAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getEmailSentAt())))")
  @Mapping(
      target = "resentAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getResentAt())))")
  @Mapping(target = "role", expression = "java(parseRole(record.getRole()))")
  TeamInvitation toDomain(TeamInvitationsRecord record);

  @Mapping(target = "expiresAt", expression = "java(toLocalDateTime(invitation.getExpiresAt()))")
  @Mapping(target = "invitedAt", expression = "java(toLocalDateTime(invitation.getInvitedAt()))")
  @Mapping(
      target = "acceptedAt",
      expression = "java(toLocalDateTime(invitation.getAcceptedAt().orElse(null)))")
  @Mapping(
      target = "emailSentAt",
      expression = "java(toLocalDateTime(invitation.getEmailSentAt().orElse(null)))")
  @Mapping(
      target = "resentAt",
      expression = "java(toLocalDateTime(invitation.getResentAt().orElse(null)))")
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "role", expression = "java(invitation.getRole().name())")
  TeamInvitationsRecord toRecord(TeamInvitation invitation);

  List<TeamInvitation> toDomainList(List<TeamInvitationsRecord> records);

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
