package com.buurman.dto.request;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateContractPaymentInstructionRequest(
    @Nullable String paymentInstructionIdentifier,
    @Nullable Boolean isCustom,
    @Nullable String customName,
    @Nullable String customDescription,
    @Nullable String customPaymentMethod,
    @Nullable String customBankName,
    @Nullable String customAccountHolderName,
    @Nullable @Size(max = 34) String customIban,
    @Nullable @Size(max = 11) String customBicSwift,
    @Nullable String customAccountNumber,
    @Nullable String customRoutingNumber,
    @Nullable String customPaymentReference,
    @Nullable String customAdditionalDetails,
    @NotNull(message = "Effective from date is required") LocalDate effectiveFrom,
    @Nullable String notes) {}
