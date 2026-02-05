package com.buurman.dto.response;

import com.buurman.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentSummary(
        String identifier,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        LocalDate paymentDate,
        Payment.PaymentStatus status
) {
}
