package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.Expense;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.ExpenseResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface ExpenseMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(
      target = "amount",
      expression = "java(com.buurman.util.MoneyAmount.of(request.amount(), request.currency()))")
  Expense toEntity(CreateExpenseRequest request);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "documents", ignore = true)
  @Mapping(target = "amount", expression = "java(expense.getAmount().value())")
  @Mapping(target = "currency", expression = "java(expense.getAmount().currency())")
  ExpenseResponse toResponse(Expense expense);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(
      target = "amount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.amount().orElse(expense.getAmount().value()),"
              + " request.currency().orElse(expense.getAmount().currency())))")
  void updateEntity(@MappingTarget Expense expense, UpdateExpenseRequest request);
}
