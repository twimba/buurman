package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyTax;
import com.buurman.util.Generated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Generated
public record UpdatePropertyTaxRequest(
    Optional<PropertyTax.TaxType> taxType,
    Optional<String> authority,
    Optional<@Positive(message = "Annual amount must be positive") BigDecimal> annualAmount,
    Optional<String> currency,
    Optional<String> paymentFrequency,
    Optional<String> dueMonths,
    Optional<
            @Min(value = 1900, message = "Tax year must be at least 1900") @Max(value = 2100, message = "Tax year must be at most 2100") Integer>
        taxYear,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PropertyTax.TaxStatus> status,
    Optional<String> notes) {}
