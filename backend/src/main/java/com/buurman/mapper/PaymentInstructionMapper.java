package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.PaymentInstruction;
import com.buurman.dto.request.CreatePaymentInstructionRequest;
import com.buurman.dto.request.UpdatePaymentInstructionRequest;
import com.buurman.dto.response.PaymentInstructionResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PaymentInstructionMapper {

  @Mapping(
      target = "paymentMethod",
      expression = "java(pi.getPaymentMethod() != null ? pi.getPaymentMethod().name() : null)")
  PaymentInstructionResponse toResponse(PaymentInstruction pi);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  PaymentInstruction toEntity(CreatePaymentInstructionRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(@MappingTarget PaymentInstruction pi, UpdatePaymentInstructionRequest request);
}
