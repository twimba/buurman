package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.PaymentInstruction.PaymentMethod;

import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record UpdatePaymentInstructionRequest(
    Optional<String> name,
    Optional<String> description,
    Optional<PaymentMethod> paymentMethod,
    Optional<String> bankName,
    Optional<String> accountHolderName,
    Optional<@Size(max = 34) String> iban,
    Optional<@Size(max = 11) String> bicSwift,
    Optional<String> accountNumber,
    Optional<String> routingNumber,
    Optional<String> paymentReference,
    Optional<String> additionalDetails,
    Optional<Boolean> isDefault) {}
