package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyValuation;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Generated
public record CreatePropertyValuationRequest(
    @NotNull(message = "Valuation type is required") PropertyValuation.ValuationType valuationType,
    @NotNull(message = "Valuation date is required") LocalDate valuationDate,
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotBlank(message = "Currency is required") String currency,
    Optional<String> source,
    Optional<String> notes) {}
