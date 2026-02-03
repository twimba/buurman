package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialOverviewRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        List<UUID> propertyIds,
        String currency
) {}
