package com.buurman.mapper;

import com.buurman.domain.Property;
import com.buurman.jooq.generated.tables.records.PropertiesRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.ZoneOffset.UTC;

@Mapper(componentModel = "spring")
public interface PropertyRecordMapper {

    @Mapping(target = "propertyType", expression = "java(toPropertyType(record.getPropertyType()))")
    @Mapping(target = "status", expression = "java(toPropertyStatus(record.getStatus()))")
    @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
    @Mapping(target = "deletedAt", expression = "java(toInstant(record.getDeletedAt()))")
    Property toDomain(PropertiesRecord record);

    @Mapping(target = "propertyType", expression = "java(fromPropertyType(property.getPropertyType()))")
    @Mapping(target = "status", expression = "java(fromPropertyStatus(property.getStatus()))")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(property.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(property.getUpdatedAt()))")
    @Mapping(target = "deletedAt", expression = "java(toLocalDateTime(property.getDeletedAt()))")
    PropertiesRecord toRecord(Property property);

    List<Property> toDomainList(List<PropertiesRecord> records);

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
    }

    default Property.PropertyType toPropertyType(String value) {
        return value == null ? null : Property.PropertyType.valueOf(value);
    }

    default String fromPropertyType(Property.PropertyType type) {
        return type == null ? null : type.name();
    }

    default Property.PropertyStatus toPropertyStatus(String value) {
        return value == null ? null : Property.PropertyStatus.valueOf(value);
    }

    default String fromPropertyStatus(Property.PropertyStatus status) {
        return status == null ? null : status.name();
    }
}
