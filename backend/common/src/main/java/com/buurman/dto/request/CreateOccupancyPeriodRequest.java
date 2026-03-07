package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateOccupancyPeriodRequest(
    @NotNull(message = "Start date is required") LocalDate startDate,
    @NotNull(message = "Occupancy type is required") OccupancyType type,
    Optional<LocalDate> endDate,
    Optional<@Size(max = 255) String> occupantName,
    Optional<@PositiveOrZero BigDecimal> monthlyImputedRent,
    Optional<@Size(max = 500) String> notes) {}
