package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Generated
public record GeneratePaymentsRequest(
    @NotNull(message = "Count is required") @Min(value = 1, message = "Count must be at least 1") @Max(value = 24, message = "Count must not exceed 24") Integer count,
    Optional<Boolean> markAsPaid,
    Optional<LocalDate> paymentDate) {}
