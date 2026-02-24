package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record TransactionHistoryRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    Optional<TransactionType> type,
    Optional<List<UUID>> propertyIds,
    Optional<List<String>> categories,
    int page,
    int size,
    Optional<String> sort) {
  public TransactionHistoryRequest {
    type = Objects.requireNonNullElse(type, Optional.empty());
    propertyIds = Objects.requireNonNullElse(propertyIds, Optional.empty());
    categories = Objects.requireNonNullElse(categories, Optional.empty());
    sort = Objects.requireNonNullElse(sort, Optional.empty());
  }

  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

  public enum TransactionType {
    INCOME,
    EXPENSE,
    ALL
  }
}
