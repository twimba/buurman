package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.FinancingPayment;
import com.buurman.domain.Ulid;

public record FinancingPaymentResponse(
    Ulid identifier,
    Ulid financingIdentifier,
    LocalDate paymentDate,
    BigDecimal totalAmount,
    Optional<BigDecimal> principalAmount,
    Optional<BigDecimal> interestAmount,
    Optional<BigDecimal> escrowAmount,
    Optional<BigDecimal> extraPayment,
    String currency,
    FinancingPayment.PaymentStatus status,
    Optional<String> notes,
    boolean balanceDeducted,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
