package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Generated
public record CreatePaymentRequest(
    @NotNull(message = "Contract identifier is required") ContractIdentifier contractIdentifier,
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotBlank(message = "Currency is required") String currency,
    @NotNull(message = "Due date is required") LocalDate dueDate,
    Optional<String> notes,
    Optional<Boolean> markAsPaid,
    Optional<LocalDate> paymentDate) {}
