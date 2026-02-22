package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Payment;

public record PaymentResponse(
    String identifier,
    @Nullable ContractSummary contract,
    @Nullable TenantSummary tenant,
    @Nullable PropertySummary property,
    BigDecimal amount,
    String currency,
    @Nullable BigDecimal receivedAmount,
    @Nullable BigDecimal balance,
    @Nullable LocalDate paymentDate,
    LocalDate dueDate,
    Payment.PaymentStatus status,
    @Nullable String notes,
    @Nullable DocumentResponse proofOfPayment,
    @Nullable DocumentResponse receipt,
    List<PaymentReceivalResponse> receivals,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
