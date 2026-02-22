package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Tenant;
import com.buurman.jooq.generated.tables.records.TenantsRecord;

@Mapper(componentModel = "spring")
public interface TenantRecordMapper {

  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toInstant(record.getDeletedAt()))")
  Tenant toDomain(TenantsRecord record);

  @Mapping(target = "createdAt", expression = "java(toLocalDateTime(tenant.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(tenant.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toLocalDateTime(tenant.getDeletedAt()))")
  TenantsRecord toRecord(Tenant tenant);

  List<Tenant> toDomainList(List<TenantsRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
