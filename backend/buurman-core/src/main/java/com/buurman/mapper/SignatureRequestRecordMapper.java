package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.SignatureRequest;
import com.buurman.jooq.generated.tables.records.SignatureRequestsRecord;

@Mapper(componentModel = "spring")
public interface SignatureRequestRecordMapper {

  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "signedDocumentId",
      expression = "java(java.util.Optional.ofNullable(record.getSignedDocumentId()))")
  @Mapping(
      target = "status",
      expression = "java(com.buurman.domain.SignatureRequestStatus.valueOf(record.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(
      target = "deletedAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getDeletedAt())))")
  SignatureRequest toDomain(SignatureRequestsRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
