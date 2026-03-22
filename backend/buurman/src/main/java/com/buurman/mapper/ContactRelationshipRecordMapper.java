package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.ContactRelationship;
import com.buurman.jooq.generated.tables.records.ContactRelationshipsRecord;

@Mapper(componentModel = "spring")
public interface ContactRelationshipRecordMapper {

  @Mapping(
      target = "relationshipType",
      expression =
          "java(com.buurman.domain.RelationshipType.valueOf(record.getRelationshipType()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "notes",
      expression = "java(java.util.Optional.ofNullable(record.getNotes()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
  ContactRelationship toDomain(ContactRelationshipsRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime).map(dt -> dt.toInstant(UTC));
  }
}
