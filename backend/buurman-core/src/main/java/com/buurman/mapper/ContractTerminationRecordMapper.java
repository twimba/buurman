package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.ContractTermination;
import com.buurman.jooq.generated.tables.records.ContractTerminationsRecord;

@Mapper(componentModel = "spring")
public interface ContractTerminationRecordMapper {

  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "givenBy",
      expression = "java(com.buurman.domain.TerminationGivenBy.valueOf(record.getGivenBy()))")
  @Mapping(
      target = "groundCode",
      expression = "java(java.util.Optional.ofNullable(record.getGroundCode()))")
  @Mapping(
      target = "overrideReason",
      expression = "java(java.util.Optional.ofNullable(record.getOverrideReason()))")
  @Mapping(
      target = "inspectionDate",
      expression = "java(java.util.Optional.ofNullable(record.getInspectionDate()))")
  @Mapping(
      target = "noticeLetterDocumentId",
      expression = "java(java.util.Optional.ofNullable(record.getNoticeLetterDocumentId()))")
  @Mapping(
      target = "status",
      expression = "java(com.buurman.domain.ContractTerminationStatus.valueOf(record.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  ContractTermination toDomain(ContractTerminationsRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
