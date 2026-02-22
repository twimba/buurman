package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record ContractPaymentInstructionResponse(
    String identifier,
    @Nullable String paymentInstructionIdentifier,
    @Nullable Boolean isCustom,
    @Nullable String name,
    @Nullable String description,
    @Nullable String paymentMethod,
    @Nullable String bankName,
    @Nullable String accountHolderName,
    @Nullable String iban,
    @Nullable String bicSwift,
    @Nullable String accountNumber,
    @Nullable String routingNumber,
    @Nullable String paymentReference,
    @Nullable String additionalDetails,
    @Nullable LocalDate effectiveFrom,
    @Nullable LocalDate effectiveTo,
    @Nullable String notes,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
