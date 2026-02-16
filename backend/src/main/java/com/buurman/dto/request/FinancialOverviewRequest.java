package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record FinancialOverviewRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    List<UUID> propertyIds,
    String currency) {
  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }
}
