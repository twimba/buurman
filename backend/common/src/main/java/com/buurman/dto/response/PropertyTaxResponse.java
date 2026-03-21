package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyTax;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record PropertyTaxResponse(
    Sid identifier,
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
