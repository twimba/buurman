package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record FinancialOverviewRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    Optional<List<UUID>> propertyIds,
    Optional<String> currency) {
  public FinancialOverviewRequest {
    propertyIds = Objects.requireNonNullElse(propertyIds, Optional.empty());
    currency = Objects.requireNonNullElse(currency, Optional.empty());
  }

  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }
}
