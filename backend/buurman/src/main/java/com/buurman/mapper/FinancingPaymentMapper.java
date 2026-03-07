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
  @Mapping(
      target = "totalAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.totalAmount(), request.currency()))")
  FinancingPayment toEntity(CreateFinancingPaymentRequest request);

  @Mapping(target = "financingIdentifier", ignore = true)
  @Mapping(target = "totalAmount", expression = "java(payment.getTotalAmount().value())")
  @Mapping(target = "currency", expression = "java(payment.getTotalAmount().currency())")
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
  @Mapping(
      target = "totalAmount",
      expression =
          "java(com.buurman.util.MoneyAmount.of(request.totalAmount().orElse(payment.getTotalAmount().value()),"
              + " request.currency().orElse(payment.getTotalAmount().currency())))")
  void updateEntity(@MappingTarget FinancingPayment payment, UpdateFinancingPaymentRequest request);
}
