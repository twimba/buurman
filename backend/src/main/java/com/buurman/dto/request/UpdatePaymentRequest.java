package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.buurman.domain.Payment;

import jakarta.validation.constraints.Positive;

public record UpdatePaymentRequest(
    @Positive(message = "Amount must be positive") BigDecimal amount,
    String currency,
    LocalDate dueDate,
    Payment.PaymentStatus status,
    String notes) {}
