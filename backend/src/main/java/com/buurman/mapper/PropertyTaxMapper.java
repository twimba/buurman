package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PropertyTax;
import com.buurman.dto.request.CreatePropertyTaxRequest;
import com.buurman.dto.request.UpdatePropertyTaxRequest;
import com.buurman.dto.response.PropertyTaxResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyTaxMapper {

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
      target = "annualAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.annualAmount(), request.currency()))")
  PropertyTax toEntity(CreatePropertyTaxRequest request);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "annualAmount", expression = "java(tax.getAnnualAmount().value())")
  @Mapping(target = "currency", expression = "java(tax.getAnnualAmount().currency())")
  PropertyTaxResponse toResponse(PropertyTax tax);

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
      target = "annualAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.annualAmount().orElse(tax.getAnnualAmount().value()),"
              + " request.currency().orElse(tax.getAnnualAmount().currency())))")
  void updateEntity(@MappingTarget PropertyTax tax, UpdatePropertyTaxRequest request);
}
