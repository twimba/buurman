package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.FinancingPayment;

public record FinancingPaymentResponse(
    String identifier,
    String financingIdentifier,
    LocalDate paymentDate,
    BigDecimal totalAmount,
    Optional<BigDecimal> principalAmount,
    Optional<BigDecimal> interestAmount,
    Optional<BigDecimal> escrowAmount,
    Optional<BigDecimal> extraPayment,
    String currency,
    FinancingPayment.PaymentStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
