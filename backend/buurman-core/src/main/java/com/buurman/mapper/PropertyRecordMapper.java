package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyType;
import com.buurman.jooq.generated.tables.records.PropertiesRecord;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyRecordMapper {

  @Mapping(
      target = "propertyCategory",
      expression = "java(toPropertyCategory(record.getPropertyCategory()))")
  @Mapping(target = "propertyType", expression = "java(toPropertyType(record.getPropertyType()))")
  @Mapping(
      target = "allocationBasis",
      expression = "java(toAllocationBasis(record.getAllocationBasis()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  Property toDomain(PropertiesRecord record);

  @Mapping(
      target = "propertyCategory",
      expression = "java(fromPropertyCategory(property.getPropertyCategory()))")
  @Mapping(
      target = "propertyType",
      expression = "java(fromPropertyType(property.getPropertyType()))")
  @Mapping(target = "allocationBasis", expression = "java(property.getAllocationBasis().name())")
  @Mapping(target = "createdAt", expression = "java(toLocalDateTime(property.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(property.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(fromOptionalInstant(property.getDeletedAt()))")
  @Mapping(target = "identifier", expression = "java(property.getIdentifier().orElse(null))")
  PropertiesRecord toRecord(Property property);

  List<Property> toDomainList(List<PropertiesRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime == null ? null : localDateTime.toInstant(UTC));
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }

  default @Nullable LocalDateTime fromOptionalInstant(Optional<Instant> value) {
    return value.map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null);
  }

  default @Nullable PropertyCategory toPropertyCategory(@Nullable String value) {
    return value == null ? null : PropertyCategory.valueOf(value);
  }

  default @Nullable String fromPropertyCategory(@Nullable PropertyCategory category) {
    return category == null ? null : category.name();
  }

  default @Nullable PropertyType toPropertyType(@Nullable String value) {
    return value == null ? null : PropertyType.valueOf(value);
  }

  default @Nullable String fromPropertyType(@Nullable PropertyType type) {
    return type == null ? null : type.name();
  }

  default AllocationBasis toAllocationBasis(@Nullable String value) {
    return value == null ? AllocationBasis.EQUAL : AllocationBasis.valueOf(value);
  }
}
