package com.buurman.dto.request;

import com.buurman.domain.Payment;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePaymentRequest(
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String currency,

        LocalDate dueDate,

        Payment.PaymentStatus status,

        String notes
) {
}
