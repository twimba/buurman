package com.buurman.dto.response;

import com.buurman.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentSummary(
        UUID id,
        String identifier,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        LocalDate paymentDate,
        Payment.PaymentStatus status
) {
}
