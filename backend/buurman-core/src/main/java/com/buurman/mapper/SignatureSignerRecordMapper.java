package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.SignatureSigner;
import com.buurman.jooq.generated.tables.records.SignatureSignersRecord;

@Mapper(componentModel = "spring")
public interface SignatureSignerRecordMapper {

  @Mapping(
      target = "contactId",
      expression = "java(java.util.Optional.ofNullable(record.getContactId()))")
  @Mapping(
      target = "status",
      expression = "java(com.buurman.domain.SignatureSignerStatus.valueOf(record.getStatus()))")
  @Mapping(
      target = "signedAt",
      expression = "java(java.util.Optional.ofNullable(toInstant(record.getSignedAt())))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  SignatureSigner toDomain(SignatureSignersRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
