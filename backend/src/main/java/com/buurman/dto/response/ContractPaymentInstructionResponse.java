package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record ContractPaymentInstructionResponse(
        String identifier,
        String paymentInstructionIdentifier,
        Boolean isCustom,
        String name,
        String description,
        String paymentMethod,
        String bankName,
        String accountHolderName,
        String iban,
        String bicSwift,
        String accountNumber,
        String routingNumber,
        String paymentReference,
        String additionalDetails,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
