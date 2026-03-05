package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PropertyInsurance;
import com.buurman.dto.request.CreatePropertyInsuranceRequest;
import com.buurman.dto.request.UpdatePropertyInsuranceRequest;
import com.buurman.dto.response.PropertyInsuranceResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyInsuranceMapper {

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
      target = "annualPremium",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.annualPremium(),"
              + " request.annualPremiumCurrency()))")
  @Mapping(
      target = "coverageAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.coverageAmount().orElse(null),"
              + " request.coverageAmountCurrency().orElse(null)))")
  PropertyInsurance toEntity(CreatePropertyInsuranceRequest request);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "annualPremium", expression = "java(insurance.getAnnualPremium().value())")
  @Mapping(
      target = "annualPremiumCurrency",
      expression = "java(insurance.getAnnualPremium().currency())")
  @Mapping(
      target = "coverageAmount",
      expression = "java(insurance.getCoverageAmount().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "coverageAmountCurrency",
      expression =
          "java(insurance.getCoverageAmount().map(com.buurman.util.MoneyAmount::currency))")
  PropertyInsuranceResponse toResponse(PropertyInsurance insurance);

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
      target = "annualPremium",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.annualPremium().orElse(insurance.getAnnualPremium().value()),"
              + " request.annualPremiumCurrency().orElse(insurance.getAnnualPremium().currency())))")
  @Mapping(
      target = "coverageAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.coverageAmount().orElse(null),"
              + " request.coverageAmountCurrency().orElse(null)))")
  void updateEntity(
      @MappingTarget PropertyInsurance insurance, UpdatePropertyInsuranceRequest request);
}
