package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Payment;

public record PaymentSummary(
    String identifier,
    BigDecimal amount,
    String currency,
    LocalDate dueDate,
    @Nullable LocalDate paymentDate,
    Payment.PaymentStatus status) {}
