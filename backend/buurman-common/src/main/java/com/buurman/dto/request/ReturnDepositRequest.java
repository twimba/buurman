package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record ReturnDepositRequest(
    @NotNull(message = "Return date is required") LocalDate returnedDate,
    /** Defaults to the refundable amount (deposit minus deductions minus already returned). */
    Optional<@Positive(message = "Amount must be positive") BigDecimal> returnedAmount,
    @Size(max = 2000) Optional<String> notes) {}
