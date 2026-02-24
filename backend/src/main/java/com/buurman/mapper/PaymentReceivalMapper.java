package com.buurman.mapper;

import org.mapstruct.Mapper;

import com.buurman.domain.PaymentReceival;
import com.buurman.dto.response.PaymentReceivalResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PaymentReceivalMapper {

  PaymentReceivalResponse toResponse(PaymentReceival receival);
}
