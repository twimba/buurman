package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.FinancingPayment;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@SkipTestCoverage
public record UpdateFinancingPaymentRequest(
    Optional<LocalDate> paymentDate,
    Optional<@Positive(message = "Total amount must be positive") BigDecimal> totalAmount,
    Optional<@PositiveOrZero(message = "Principal amount must be zero or positive") BigDecimal>
        principalAmount,
    Optional<@PositiveOrZero(message = "Interest amount must be zero or positive") BigDecimal>
        interestAmount,
    Optional<@PositiveOrZero(message = "Escrow amount must be zero or positive") BigDecimal>
        escrowAmount,
    Optional<@PositiveOrZero(message = "Extra payment must be zero or positive") BigDecimal>
        extraPayment,
    Optional<String> currency,
    Optional<FinancingPayment.PaymentStatus> status,
    Optional<String> notes,
    Optional<Boolean> deductFromBalance) {}
