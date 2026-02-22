package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.PaymentInstruction.PaymentMethod;

import jakarta.validation.constraints.Size;

public record UpdatePaymentInstructionRequest(
    @Nullable String name,
    @Nullable String description,
    @Nullable PaymentMethod paymentMethod,
    @Nullable String bankName,
    @Nullable String accountHolderName,
    @Nullable @Size(max = 34) String iban,
    @Nullable @Size(max = 11) String bicSwift,
    @Nullable String accountNumber,
    @Nullable String routingNumber,
    @Nullable String paymentReference,
    @Nullable String additionalDetails,
    @Nullable Boolean isDefault) {}
