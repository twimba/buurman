package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Tenant;
import com.buurman.jooq.generated.tables.records.TenantsRecord;

@Mapper(componentModel = "spring")
public interface TenantRecordMapper {

  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
  @Mapping(target = "phone", expression = "java(java.util.Optional.ofNullable(record.getPhone()))")
  @Mapping(
      target = "taxNumber",
      expression = "java(java.util.Optional.ofNullable(record.getTaxNumber()))")
  @Mapping(
      target = "idNumber",
      expression = "java(java.util.Optional.ofNullable(record.getIdNumber()))")
  @Mapping(
      target = "additionalInfo",
      expression = "java(java.util.Optional.ofNullable(record.getAdditionalInfo()))")
  @Mapping(
      target = "currentPropertyId",
      expression = "java(java.util.Optional.ofNullable(record.getCurrentPropertyId()))")
  Tenant toDomain(TenantsRecord record);

  @Mapping(target = "createdAt", expression = "java(toLocalDateTime(tenant.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(tenant.getUpdatedAt()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(tenant.getDeletedAt().map(i ->"
              + " java.time.LocalDateTime.ofInstant(i, java.time.ZoneOffset.UTC)).orElse(null))")
  @Mapping(target = "phone", expression = "java(tenant.getPhone().orElse(null))")
  @Mapping(target = "taxNumber", expression = "java(tenant.getTaxNumber().orElse(null))")
  @Mapping(target = "idNumber", expression = "java(tenant.getIdNumber().orElse(null))")
  @Mapping(target = "additionalInfo", expression = "java(tenant.getAdditionalInfo().orElse(null))")
  @Mapping(
      target = "currentPropertyId",
      expression = "java(tenant.getCurrentPropertyId().orElse(null))")
  TenantsRecord toRecord(Tenant tenant);

  List<Tenant> toDomainList(List<TenantsRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime).map(dt -> dt.toInstant(UTC));
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
