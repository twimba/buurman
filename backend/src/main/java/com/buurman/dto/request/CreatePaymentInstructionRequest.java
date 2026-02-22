package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.PaymentInstruction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentInstructionRequest(
    @NotBlank(message = "Name is required") String name,
    @Nullable String description,
    @NotNull(message = "Payment method is required") PaymentInstruction.PaymentMethod paymentMethod,
    @Nullable String bankName,
    @Nullable String accountHolderName,
    @Nullable @Size(max = 34) String iban,
    @Nullable @Size(max = 11) String bicSwift,
    @Nullable String accountNumber,
    @Nullable String routingNumber,
    @Nullable String paymentReference,
    @Nullable String additionalDetails,
    @Nullable Boolean isDefault) {}
