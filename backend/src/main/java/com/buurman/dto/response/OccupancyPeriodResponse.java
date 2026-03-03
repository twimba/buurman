package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyOccupancyPeriod.OccupancyEndReason;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;
import com.buurman.domain.Ulid;

public record OccupancyPeriodResponse(
    Ulid identifier,
    Ulid propertyIdentifier,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    OccupancyType type,
    Optional<String> occupantName,
    Optional<BigDecimal> monthlyImputedRent,
    Optional<OccupancyEndReason> endReason,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
