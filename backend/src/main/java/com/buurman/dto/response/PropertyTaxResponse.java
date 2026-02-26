package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyTax;

public record PropertyTaxResponse(
    String identifier,
    Optional<PropertySummary> property,
    PropertyTax.TaxType taxType,
    Optional<String> authority,
    BigDecimal annualAmount,
    String currency,
    String paymentFrequency,
    Optional<String> dueMonths,
    Optional<Integer> taxYear,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    PropertyTax.TaxStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
