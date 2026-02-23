package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public record ContractPaymentInstructionResponse(
    String identifier,
    Optional<String> paymentInstructionIdentifier,
    Optional<Boolean> isCustom,
    Optional<String> name,
    Optional<String> description,
    Optional<String> paymentMethod,
    Optional<String> bankName,
    Optional<String> accountHolderName,
    Optional<String> iban,
    Optional<String> bicSwift,
    Optional<String> accountNumber,
    Optional<String> routingNumber,
    Optional<String> paymentReference,
    Optional<String> additionalDetails,
    Optional<LocalDate> effectiveFrom,
    Optional<LocalDate> effectiveTo,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
