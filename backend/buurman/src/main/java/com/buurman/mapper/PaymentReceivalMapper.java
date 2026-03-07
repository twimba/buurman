package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.PaymentReceival;
import com.buurman.dto.response.PaymentReceivalResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PaymentReceivalMapper {

  @Mapping(target = "amount", expression = "java(receival.getAmount().value())")
  @Mapping(target = "currency", expression = "java(receival.getAmount().currency())")
  PaymentReceivalResponse toResponse(PaymentReceival receival);
}
