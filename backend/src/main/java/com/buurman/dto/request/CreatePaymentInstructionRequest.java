package com.buurman.dto.request;

import com.buurman.domain.PaymentInstruction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentInstructionRequest(
    @NotBlank(message = "Name is required") String name,
    String description,
    @NotNull(message = "Payment method is required") PaymentInstruction.PaymentMethod paymentMethod,
    String bankName,
    String accountHolderName,
    @Size(max = 34) String iban,
    @Size(max = 11) String bicSwift,
    String accountNumber,
    String routingNumber,
    String paymentReference,
    String additionalDetails,
    Boolean isDefault) {}
