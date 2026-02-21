package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(
    @NotNull(message = "Contract identifier is required") String contractIdentifier,
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotBlank(message = "Currency is required") String currency,
    @NotNull(message = "Due date is required") LocalDate dueDate,
    String notes,
    Boolean markAsPaid,
    LocalDate paymentDate) {}
