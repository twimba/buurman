package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PropertyValuation;
import com.buurman.dto.request.CreatePropertyValuationRequest;
import com.buurman.dto.request.UpdatePropertyValuationRequest;
import com.buurman.dto.response.PropertyValuationResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyValuationMapper {

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
      target = "amount",
      expression = "java(com.buurman.util.MoneyAmount.of(request.amount(), request.currency()))")
  PropertyValuation toEntity(CreatePropertyValuationRequest request);

  @Mapping(target = "amount", expression = "java(valuation.getAmount().value())")
  @Mapping(target = "currency", expression = "java(valuation.getAmount().currency())")
  PropertyValuationResponse toResponse(PropertyValuation valuation);

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
      target = "amount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.amount().orElse(valuation.getAmount().value()),"
              + " request.currency().orElse(valuation.getAmount().currency())))")
  void updateEntity(
      @MappingTarget PropertyValuation valuation, UpdatePropertyValuationRequest request);
}
