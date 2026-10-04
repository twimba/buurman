package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.ContractLeaseClause;
import com.buurman.jooq.generated.tables.records.ContractLeaseClausesRecord;

@Mapper(componentModel = "spring")
public interface ContractLeaseClauseRecordMapper {

  @Mapping(
      target = "createdAt",
      expression = "java(record.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))")
  @Mapping(
      target = "updatedAt",
      expression = "java(record.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC))")
  ContractLeaseClause toDomain(ContractLeaseClausesRecord record);
}
