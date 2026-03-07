package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePropertyFeeRequest(
    @NotNull(message = "Fee type is required") PropertyFee.FeeType feeType,
    Optional<String> name,
    @NotNull(message = "Annual amount is required") @Positive(message = "Annual amount must be positive") BigDecimal annualAmount,
    @NotBlank(message = "Currency is required") String currency,
    @NotBlank(message = "Payment frequency is required") String paymentFrequency,
    Optional<String> dueMonths,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PropertyFee.FeeStatus> status,
    Optional<String> notes) {}
