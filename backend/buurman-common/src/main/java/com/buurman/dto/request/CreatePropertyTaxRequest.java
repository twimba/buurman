package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyTax;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@SkipTestCoverage
public record CreatePropertyTaxRequest(
    @NotNull(message = "Tax type is required") PropertyTax.TaxType taxType,
    Optional<String> authority,
    @NotNull(message = "Annual amount is required") @Positive(message = "Annual amount must be positive") BigDecimal annualAmount,
    @NotBlank(message = "Currency is required") String currency,
    @NotBlank(message = "Payment frequency is required") String paymentFrequency,
    Optional<String> dueMonths,
    Optional<
            @Min(value = 1900, message = "Tax year must be at least 1900") @Max(value = 2100, message = "Tax year must be at most 2100") Integer>
        taxYear,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PropertyTax.TaxStatus> status,
    Optional<String> notes) {}
