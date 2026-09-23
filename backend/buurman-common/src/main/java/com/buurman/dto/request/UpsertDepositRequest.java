package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record UpsertDepositRequest(
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    Optional<LocalDate> receivedDate,
    @Size(max = 255) Optional<String> heldWhere,
    Optional<LocalDate> returnDueDate,
    @Size(max = 2000) Optional<String> notes) {}
