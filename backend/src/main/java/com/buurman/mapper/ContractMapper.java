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

@Mapper(componentModel = "spring")
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
  @Mapping(target = "autoRenewal", defaultExpression = "java(false)")
  @Mapping(target = "renewalNoticeDays", defaultExpression = "java(30)")
  @Mapping(target = "terminationNoticeDays", defaultExpression = "java(30)")
  Contract toEntity(CreateContractRequest request);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "parties", ignore = true)
  @Mapping(target = "primaryTenant", ignore = true)
  ContractResponse toResponse(Contract contract);

  @Mapping(target = "property", ignore = true)
  @Mapping(target = "primaryTenant", ignore = true)
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
  void updateEntity(@MappingTarget Contract contract, UpdateContractRequest request);
}
