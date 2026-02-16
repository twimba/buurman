package com.buurman.dto.response;

import java.time.Instant;

public record PaymentInstructionResponse(
    String identifier,
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
    Boolean isDefault,
    Instant createdAt,
    Instant updatedAt) {}
