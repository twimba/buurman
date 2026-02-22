package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record RentPeriodResponse(
    String identifier,
    BigDecimal rentAmount,
    String currency,
    LocalDate effectiveFrom,
    @Nullable LocalDate effectiveTo,
    @Nullable String notes,
    @Nullable BigDecimal percentageChange,
    Instant createdAt) {}
