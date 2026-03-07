package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFee;

import jakarta.validation.constraints.Positive;

public record UpdatePropertyFeeRequest(
    Optional<PropertyFee.FeeType> feeType,
    Optional<String> name,
    Optional<@Positive(message = "Annual amount must be positive") BigDecimal> annualAmount,
    Optional<String> currency,
    Optional<String> paymentFrequency,
    Optional<String> dueMonths,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PropertyFee.FeeStatus> status,
    Optional<String> notes) {}
