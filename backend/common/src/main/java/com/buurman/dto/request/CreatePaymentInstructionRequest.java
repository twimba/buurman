package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.PaymentInstruction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record CreatePaymentInstructionRequest(
    @NotBlank(message = "Name is required") String name,
    Optional<String> description,
    @NotNull(message = "Payment method is required") PaymentInstruction.PaymentMethod paymentMethod,
    Optional<String> bankName,
    Optional<String> accountHolderName,
    Optional<@Size(max = 34) String> iban,
    Optional<@Size(max = 11) String> bicSwift,
    Optional<String> accountNumber,
    Optional<String> routingNumber,
    Optional<String> paymentReference,
    Optional<String> additionalDetails,
    Optional<Boolean> isDefault) {}
