package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyValuation;
import com.buurman.util.Generated;

import jakarta.validation.constraints.Positive;

@Generated
public record UpdatePropertyValuationRequest(
    Optional<PropertyValuation.ValuationType> valuationType,
    Optional<LocalDate> valuationDate,
    Optional<@Positive(message = "Amount must be positive") BigDecimal> amount,
    Optional<String> currency,
    Optional<String> source,
    Optional<String> notes) {}
