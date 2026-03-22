package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.ContactNote;
import com.buurman.jooq.generated.tables.records.ContactNotesRecord;

@Mapper(componentModel = "spring")
public interface ContactNoteRecordMapper {

  @Mapping(
      target = "interactionType",
      expression = "java(com.buurman.domain.InteractionType.valueOf(record.getInteractionType()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "subject",
      expression = "java(java.util.Optional.ofNullable(record.getSubject()))")
  @Mapping(
      target = "followUpDate",
      expression = "java(java.util.Optional.ofNullable(record.getFollowUpDate()))")
  @Mapping(target = "occurredAt", expression = "java(toInstant(record.getOccurredAt()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
  ContactNote toDomain(ContactNotesRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime).map(dt -> dt.toInstant(UTC));
  }
}
