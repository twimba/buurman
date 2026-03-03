package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record RentPeriodResponse(
    Ulid identifier,
    BigDecimal rentAmount,
    String currency,
    LocalDate effectiveFrom,
    Optional<LocalDate> effectiveTo,
    Optional<String> notes,
    Optional<BigDecimal> percentageChange,
    Instant createdAt) {}
