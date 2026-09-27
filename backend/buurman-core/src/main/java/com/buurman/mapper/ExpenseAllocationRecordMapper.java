package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.jooq.generated.tables.records.ExpenseAllocationsRecord;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface ExpenseAllocationRecordMapper {

  @Mapping(
      target = "amount",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(record.getAmount(),"
              + " record.getAmountCurrency()).orElseThrow())")
  @Mapping(target = "basis", expression = "java(toAllocationBasis(record.getBasis()))")
  @Mapping(target = "createdAt", expression = "java(toOptionalInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toOptionalInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  ExpenseAllocation toDomain(ExpenseAllocationsRecord record);

  default AllocationBasis toAllocationBasis(@Nullable String value) {
    return value == null ? AllocationBasis.EQUAL : AllocationBasis.valueOf(value);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime == null ? null : localDateTime.toInstant(UTC));
  }
}
