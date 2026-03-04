package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Payment;
import com.buurman.domain.Sid;

public record PaymentSummary(
    Sid identifier,
    BigDecimal amount,
    String currency,
    LocalDate dueDate,
    Optional<LocalDate> paymentDate,
    Payment.PaymentStatus status) {}
