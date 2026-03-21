package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record UpdateOccupancyPeriodRequest(
    Optional<LocalDate> startDate,
    Optional<OccupancyType> type,
    Optional<LocalDate> endDate,
    Optional<@Size(max = 255) String> occupantName,
    Optional<@PositiveOrZero BigDecimal> monthlyImputedRent,
    Optional<@Size(max = 500) String> notes) {}
