package com.buurman.mapper;

import com.buurman.domain.Payment;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PaymentSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identifier", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Payment toEntity(CreatePaymentRequest request);

    @Mapping(target = "contract", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "property", ignore = true)
    @Mapping(target = "proofOfPayment", ignore = true)
    @Mapping(target = "receipt", ignore = true)
    PaymentResponse toResponse(Payment payment);

    PaymentSummary toSummary(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identifier", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "contractId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget Payment payment, UpdatePaymentRequest request);
}
