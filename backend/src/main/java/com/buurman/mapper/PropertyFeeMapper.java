package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PropertyFee;
import com.buurman.dto.request.CreatePropertyFeeRequest;
import com.buurman.dto.request.UpdatePropertyFeeRequest;
import com.buurman.dto.response.PropertyFeeResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyFeeMapper {

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
  PropertyFee toEntity(CreatePropertyFeeRequest request);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "annualAmount", expression = "java(fee.getAnnualAmount().value())")
  @Mapping(target = "currency", expression = "java(fee.getAnnualAmount().currency())")
  PropertyFeeResponse toResponse(PropertyFee fee);

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
          "java(com.buurman.util.MoneyAmount.of(request.annualAmount().orElse(fee.getAnnualAmount().value()),"
              + " request.currency().orElse(fee.getAnnualAmount().currency())))")
  void updateEntity(@MappingTarget PropertyFee fee, UpdatePropertyFeeRequest request);
}
