package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment record with amount, due date, status, and linked contract")
public record PaymentResponse(
    @Schema(description = "Unique payment identifier", example = "pay_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
    Optional<ContractSummary> contract,
    Optional<TenantSummary> tenant,
    Optional<PropertySummary> property,
    BigDecimal amount,
    String currency,
    Optional<BigDecimal> receivedAmount,
    Optional<BigDecimal> balance,
    Optional<LocalDate> paymentDate,
    LocalDate dueDate,
    Payment.PaymentStatus status,
    Optional<String> notes,
    Optional<DocumentResponse> proofOfPayment,
    Optional<DocumentResponse> receipt,
    List<PaymentReceivalResponse> receivals,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
