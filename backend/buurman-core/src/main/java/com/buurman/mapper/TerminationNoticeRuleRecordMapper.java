package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.jooq.generated.tables.records.RentRegulationTerminationRulesRecord;

@Mapper(componentModel = "spring")
public interface TerminationNoticeRuleRecordMapper {

  @Mapping(
      target = "regionId",
      expression = "java(java.util.Optional.ofNullable(record.getRegionId()))")
  @Mapping(
      target = "partyType",
      expression = "java(com.buurman.domain.TerminationGivenBy.valueOf(record.getPartyType()))")
  @Mapping(
      target = "minTenancyMonths",
      expression = "java(java.util.Optional.ofNullable(record.getMinTenancyMonths()))")
  @Mapping(
      target = "groundsCodes",
      expression =
          "java(record.getGroundsCodes() == null ? java.util.List.<String>of() :"
              + " java.util.List.of(record.getGroundsCodes()))")
  @Mapping(
      target = "sourceUrl",
      expression = "java(java.util.Optional.ofNullable(record.getSourceUrl()))")
  @Mapping(target = "notes", expression = "java(java.util.Optional.ofNullable(record.getNotes()))")
  TerminationNoticeRule toDomain(RentRegulationTerminationRulesRecord record);
}
