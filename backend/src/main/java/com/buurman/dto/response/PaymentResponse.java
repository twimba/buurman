package com.buurman.dto.response;

import com.buurman.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String identifier,
        UUID teamId,
        ContractSummary contract,
        TenantSummary tenant,
        PropertySummary property,
        BigDecimal amount,
        String currency,
        LocalDate paymentDate,
        LocalDate dueDate,
        Payment.PaymentStatus status,
        String notes,
        DocumentResponse proofOfPayment,
        DocumentResponse receipt,
        Instant createdAt,
        Instant updatedAt
) {
}
