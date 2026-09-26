package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.jooq.generated.tables.records.UnitsRecord;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface UnitRecordMapper {

  @Mapping(target = "unitType", expression = "java(toUnitType(record.getUnitType()))")
  @Mapping(target = "status", expression = "java(toUnitStatus(record.getStatus()))")
  @Mapping(target = "implicit", expression = "java(record.getIsImplicit())")
  @Mapping(
      target = "wozValue",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(record.getWozValue(),"
              + " record.getWozValueCurrency()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  Unit toDomain(UnitsRecord record);

  default @Nullable UnitType toUnitType(@Nullable String value) {
    return value == null ? null : UnitType.valueOf(value);
  }

  default @Nullable UnitStatus toUnitStatus(@Nullable String value) {
    return value == null ? null : UnitStatus.valueOf(value);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime == null ? null : localDateTime.toInstant(UTC));
  }
}
