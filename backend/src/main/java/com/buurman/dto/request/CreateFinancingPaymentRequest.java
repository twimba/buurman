package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.FinancingPayment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateFinancingPaymentRequest(
    @NotNull(message = "Payment date is required") LocalDate paymentDate,
    @NotNull(message = "Total amount is required") @Positive(message = "Total amount must be positive") BigDecimal totalAmount,
    Optional<@PositiveOrZero(message = "Principal amount must be zero or positive") BigDecimal>
        principalAmount,
    Optional<@PositiveOrZero(message = "Interest amount must be zero or positive") BigDecimal>
        interestAmount,
    Optional<@PositiveOrZero(message = "Escrow amount must be zero or positive") BigDecimal>
        escrowAmount,
    Optional<@PositiveOrZero(message = "Extra payment must be zero or positive") BigDecimal>
        extraPayment,
    @NotBlank(message = "Currency is required") String currency,
    Optional<FinancingPayment.PaymentStatus> status,
    Optional<String> notes) {}
