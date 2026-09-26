package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
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
}
