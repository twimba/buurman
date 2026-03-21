package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyOccupancyPeriod.OccupancyEndReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record EndOccupancyPeriodRequest(
    @NotNull(message = "End date is required") LocalDate endDate,
    Optional<OccupancyEndReason> endReason,
    Optional<@Size(max = 500) String> notes) {}
