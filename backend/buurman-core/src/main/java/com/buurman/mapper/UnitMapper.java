package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.UpdateUnitRequest;
import com.buurman.dto.response.UnitResponse;
import com.buurman.dto.response.UnitSummaryResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface UnitMapper {

  @Mapping(target = "propertyIdentifier", source = "propertyIdentifier")
  @Mapping(
      target = "wozValue",
      expression = "java(unit.getWozValue().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "wozValueCurrency",
      expression = "java(unit.getWozValue().map(com.buurman.util.MoneyAmount::currency))")
  UnitResponse toResponse(Unit unit, Sid propertyIdentifier);

  UnitSummaryResponse toSummary(Unit unit);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "implicit", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(
      target = "wozValue",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.wozValue().orElse(null),"
              + " request.wozValueCurrency().orElse(null)))")
  @Mapping(
      target = "status",
      expression = "java(request.status().orElse(com.buurman.domain.UnitStatus.VACANT))")
  @Mapping(target = "version", ignore = true)
  Unit toEntity(CreateUnitRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "implicit", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "status", expression = "java(request.status().orElse(unit.getStatus()))")
  @Mapping(
      target = "wozValue",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable("
              + "request.wozValue().orElse(unit.getWozValue().map(com.buurman.util.MoneyAmount::value).orElse(null)),"
              + " request.wozValueCurrency().orElse(unit.getWozValue().map(com.buurman.util.MoneyAmount::currency).orElse(null))))")
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "wozSharePct", ignore = true)
  @Mapping(target = "allocationShare", ignore = true)
  void updateEntity(@MappingTarget Unit unit, UpdateUnitRequest request);
}
