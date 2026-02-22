package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record PaymentInstructionResponse(
    String identifier,
    String name,
    @Nullable String description,
    String paymentMethod,
    @Nullable String bankName,
    @Nullable String accountHolderName,
    @Nullable String iban,
    @Nullable String bicSwift,
    @Nullable String accountNumber,
    @Nullable String routingNumber,
    @Nullable String paymentReference,
    @Nullable String additionalDetails,
    @Nullable Boolean isDefault,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
