package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Payment.PaymentStatus;

import jakarta.validation.constraints.Positive;

public record UpdatePaymentRequest(
    @Nullable @Positive(message = "Amount must be positive") BigDecimal amount,
    @Nullable String currency,
    @Nullable LocalDate dueDate,
    @Nullable PaymentStatus status,
    @Nullable String notes) {}
