package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.jooq.generated.tables.records.LeaseClauseTemplatesRecord;

@Mapper(componentModel = "spring")
public interface LeaseClauseTemplateRecordMapper {

  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(java.util.Optional.ofNullable(record.getDeletedAt()).map(dt ->"
              + " dt.toInstant(java.time.ZoneOffset.UTC)))")
  @Mapping(
      target = "createdAt",
      expression = "java(record.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))")
  @Mapping(
      target = "updatedAt",
      expression = "java(record.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC))")
  LeaseClauseTemplate toDomain(LeaseClauseTemplatesRecord record);
}
