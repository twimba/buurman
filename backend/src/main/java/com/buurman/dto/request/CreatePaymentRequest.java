package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to create a new payment record")
public record CreatePaymentRequest(
    @NotNull(message = "Contract identifier is required") String contractIdentifier,
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotBlank(message = "Currency is required") String currency,
    @NotNull(message = "Due date is required") LocalDate dueDate,
    Optional<String> notes,
    Optional<Boolean> markAsPaid,
    Optional<LocalDate> paymentDate) {}
