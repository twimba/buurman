package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record PaymentInstructionResponse(
    Ulid identifier,
    String name,
    Optional<String> description,
    String paymentMethod,
    Optional<String> bankName,
    Optional<String> accountHolderName,
    Optional<String> iban,
    Optional<String> bicSwift,
    Optional<String> accountNumber,
    Optional<String> routingNumber,
    Optional<String> paymentReference,
    Optional<String> additionalDetails,
    Optional<Boolean> isDefault,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
