package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RentPeriodResponse(
    String identifier,
    BigDecimal rentAmount,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String notes,
    BigDecimal percentageChange,
    Instant createdAt) {}
