package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.FinancingPayment;
import com.buurman.dto.request.CreateFinancingPaymentRequest;
import com.buurman.dto.request.UpdateFinancingPaymentRequest;
import com.buurman.dto.response.FinancingPaymentResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface FinancingPaymentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "financingId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "balanceDeducted", ignore = true)
  @Mapping(
      target = "status",
      expression =
          "java(request.status().orElse(com.buurman.domain.FinancingPayment.PaymentStatus.COMPLETED))")
  FinancingPayment toEntity(CreateFinancingPaymentRequest request);

  @Mapping(target = "financingIdentifier", ignore = true)
  FinancingPaymentResponse toResponse(FinancingPayment payment);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "financingId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "balanceDeducted", ignore = true)
  void updateEntity(@MappingTarget FinancingPayment payment, UpdateFinancingPaymentRequest request);
}
