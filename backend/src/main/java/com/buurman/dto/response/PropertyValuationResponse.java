package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyValuation;
import com.buurman.domain.Ulid;

public record PropertyValuationResponse(
    Ulid identifier,
    PropertyValuation.ValuationType valuationType,
    LocalDate valuationDate,
    BigDecimal amount,
    String currency,
    Optional<String> source,
    Optional<String> notes,
    Instant createdAt,
    Instant updatedAt) {}
