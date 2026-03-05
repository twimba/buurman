package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PropertyFinancing;
import com.buurman.dto.request.CreatePropertyFinancingRequest;
import com.buurman.dto.request.UpdatePropertyFinancingRequest;
import com.buurman.dto.response.PropertyFinancingResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyFinancingMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "paymentVariable", expression = "java(request.paymentVariable().orElse(false))")
  @Mapping(
      target = "status",
      expression =
          "java(request.status().orElse(com.buurman.domain.PropertyFinancing.FinancingStatus.ACTIVE))")
  @Mapping(
      target = "originalAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.originalAmount(),"
              + " request.originalAmountCurrency()))")
  PropertyFinancing toEntity(CreatePropertyFinancingRequest request);

  @Mapping(target = "propertyIdentifier", ignore = true)
  @Mapping(target = "originalAmount", expression = "java(financing.getOriginalAmount().value())")
  @Mapping(
      target = "originalAmountCurrency",
      expression = "java(financing.getOriginalAmount().currency())")
  @Mapping(
      target = "currentBalanceCurrency",
      expression =
          "java(financing.getCurrentBalance().isPresent() ?"
              + " java.util.Optional.of(financing.getOriginalAmount().currency()) :"
              + " java.util.Optional.empty())")
  @Mapping(
      target = "monthlyPaymentCurrency",
      expression =
          "java(financing.getMonthlyPayment().isPresent() ?"
              + " java.util.Optional.of(financing.getOriginalAmount().currency()) :"
              + " java.util.Optional.empty())")
  PropertyFinancingResponse toResponse(PropertyFinancing financing);

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
      target = "originalAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.originalAmount().orElse(financing.getOriginalAmount().value()),"
              + " request.originalAmountCurrency().orElse(financing.getOriginalAmount().currency())))")
  void updateEntity(
      @MappingTarget PropertyFinancing financing, UpdatePropertyFinancingRequest request);
}
