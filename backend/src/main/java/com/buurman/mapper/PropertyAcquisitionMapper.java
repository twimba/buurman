package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PropertyAcquisition;
import com.buurman.dto.request.UpsertPropertyAcquisitionRequest;
import com.buurman.dto.response.PropertyAcquisitionResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyAcquisitionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(
      target = "purchasePrice",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.purchasePrice().orElse(null),"
              + " request.purchasePriceCurrency().orElse(null)))")
  @Mapping(
      target = "closingCosts",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.closingCosts().orElse(null),"
              + " request.closingCostsCurrency().orElse(null)))")
  @Mapping(
      target = "renovationCosts",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.renovationCosts().orElse(null),"
              + " request.renovationCostsCurrency().orElse(null)))")
  @Mapping(
      target = "landValue",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.landValue().orElse(null),"
              + " request.landValueCurrency().orElse(null)))")
  PropertyAcquisition toEntity(UpsertPropertyAcquisitionRequest request);

  @Mapping(
      target = "purchasePrice",
      expression = "java(acquisition.getPurchasePrice().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "purchasePriceCurrency",
      expression =
          "java(acquisition.getPurchasePrice().map(com.buurman.util.MoneyAmount::currency))")
  @Mapping(
      target = "closingCosts",
      expression = "java(acquisition.getClosingCosts().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "closingCostsCurrency",
      expression =
          "java(acquisition.getClosingCosts().map(com.buurman.util.MoneyAmount::currency))")
  @Mapping(
      target = "renovationCosts",
      expression =
          "java(acquisition.getRenovationCosts().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "renovationCostsCurrency",
      expression =
          "java(acquisition.getRenovationCosts().map(com.buurman.util.MoneyAmount::currency))")
  @Mapping(
      target = "landValue",
      expression = "java(acquisition.getLandValue().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "landValueCurrency",
      expression = "java(acquisition.getLandValue().map(com.buurman.util.MoneyAmount::currency))")
  PropertyAcquisitionResponse toResponse(PropertyAcquisition acquisition);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(
      target = "purchasePrice",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.purchasePrice().orElse(null),"
              + " request.purchasePriceCurrency().orElse(null)))")
  @Mapping(
      target = "closingCosts",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.closingCosts().orElse(null),"
              + " request.closingCostsCurrency().orElse(null)))")
  @Mapping(
      target = "renovationCosts",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.renovationCosts().orElse(null),"
              + " request.renovationCostsCurrency().orElse(null)))")
  @Mapping(
      target = "landValue",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.landValue().orElse(null),"
              + " request.landValueCurrency().orElse(null)))")
  void updateEntity(
      @MappingTarget PropertyAcquisition acquisition, UpsertPropertyAcquisitionRequest request);
}
