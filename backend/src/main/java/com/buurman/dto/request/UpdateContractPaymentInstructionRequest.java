package com.buurman.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateContractPaymentInstructionRequest(
    String paymentInstructionIdentifier,
    Boolean isCustom,
    String customName,
    String customDescription,
    String customPaymentMethod,
    String customBankName,
    String customAccountHolderName,
    @Size(max = 34) String customIban,
    @Size(max = 11) String customBicSwift,
    String customAccountNumber,
    String customRoutingNumber,
    String customPaymentReference,
    String customAdditionalDetails,
    @NotNull(message = "Effective from date is required") LocalDate effectiveFrom,
    String notes) {}
