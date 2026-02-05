package com.buurman.mapper;

import com.buurman.domain.PaymentReceival;
import com.buurman.dto.response.PaymentReceivalResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentReceivalMapper {

    PaymentReceivalResponse toResponse(PaymentReceival receival);
}
