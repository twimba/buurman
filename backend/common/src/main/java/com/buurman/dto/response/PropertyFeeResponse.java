package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFee;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PropertyFeeResponse(
    Sid identifier,
    Optional<PropertySummary> property,
    PropertyFee.FeeType feeType,
    Optional<String> name,
    BigDecimal annualAmount,
    String currency,
    String paymentFrequency,
    Optional<String> dueMonths,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    PropertyFee.FeeStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
