package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.PaymentInstruction.PaymentMethod;

import jakarta.validation.constraints.Size;

public record UpdatePaymentInstructionRequest(
    Optional<String> name,
    Optional<String> description,
    Optional<PaymentMethod> paymentMethod,
    Optional<String> bankName,
    Optional<String> accountHolderName,
    @Size(max = 34) Optional<String> iban,
    @Size(max = 11) Optional<String> bicSwift,
    Optional<String> accountNumber,
    Optional<String> routingNumber,
    Optional<String> paymentReference,
    Optional<String> additionalDetails,
    Optional<Boolean> isDefault) {}
