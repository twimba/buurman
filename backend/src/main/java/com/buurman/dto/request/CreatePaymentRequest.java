package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
        @NotNull(message = "Contract identifier is required")
        String contractIdentifier,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        String currency,

        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        String notes
) {
}
