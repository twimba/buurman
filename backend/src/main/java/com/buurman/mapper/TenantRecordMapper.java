package com.buurman.mapper;

import com.buurman.domain.Tenant;
import com.buurman.jooq.generated.tables.records.TenantsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
