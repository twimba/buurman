package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.Contract;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.ContractSummary;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface ContractMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "countryCode", ignore = true)
  @Mapping(target = "countryMetadata", ignore = true)
  @Mapping(target = "autoRenewal", defaultExpression = "java(false)")
  @Mapping(target = "renewalNoticeDays", defaultExpression = "java(30)")
  @Mapping(target = "terminationNoticeDays", defaultExpression = "java(30)")
  @Mapping(
      target = "rentAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.rentAmount(),"
              + " request.rentAmountCurrency().orElse(\"EUR\")))")
  @Mapping(
      target = "depositAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.depositAmount().orElse(null),"
              + " request.depositAmountCurrency().orElse(null)))")
  @Mapping(
      target = "securityDeposit",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.securityDeposit().orElse(null),"
              + " request.securityDepositCurrency().orElse(null)))")
  Contract toEntity(CreateContractRequest request);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "parties", ignore = true)
  @Mapping(target = "primaryTenant", ignore = true)
  @Mapping(target = "countryMetadata", ignore = true)
  @Mapping(target = "rentAmount", expression = "java(contract.getRentAmount().value())")
  @Mapping(target = "rentAmountCurrency", expression = "java(contract.getRentAmount().currency())")
  @Mapping(
      target = "depositAmount",
      expression = "java(contract.getDepositAmount().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "depositAmountCurrency",
      expression = "java(contract.getDepositAmount().map(com.buurman.util.MoneyAmount::currency))")
  @Mapping(
      target = "securityDeposit",
      expression = "java(contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::value))")
  @Mapping(
      target = "securityDepositCurrency",
      expression =
          "java(contract.getSecurityDeposit().map(com.buurman.util.MoneyAmount::currency))")
  ContractResponse toResponse(Contract contract);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "primaryTenant", ignore = true)
  @Mapping(target = "rentAmount", expression = "java(contract.getRentAmount().value())")
  ContractSummary toSummary(Contract contract);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "countryCode", ignore = true)
  @Mapping(target = "countryMetadata", ignore = true)
  @Mapping(
      target = "rentAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.rentAmount(),"
              + " request.rentAmountCurrency().orElse(contract.getRentAmount().currency())))")
  @Mapping(
      target = "depositAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.depositAmount().orElse(null),"
              + " request.depositAmountCurrency()"
              + ".orElse(contract.getDepositAmount()"
              + ".map(com.buurman.util.MoneyAmount::currency).orElse(null))))")
  @Mapping(
      target = "securityDeposit",
      expression =
          "java(com.buurman.util.MoneyAmount.ofNullable(request.securityDeposit().orElse(null),"
              + " request.securityDepositCurrency()"
              + ".orElse(contract.getSecurityDeposit()"
              + ".map(com.buurman.util.MoneyAmount::currency).orElse(null))))")
  void updateEntity(@MappingTarget Contract contract, UpdateContractRequest request);
}
