package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Payment;

public record PaymentSummary(
    String identifier,
    BigDecimal amount,
    String currency,
    LocalDate dueDate,
    Optional<LocalDate> paymentDate,
    Payment.PaymentStatus status) {}
