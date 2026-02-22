package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record TransactionHistoryRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @Nullable TransactionType type,
    @Nullable List<UUID> propertyIds,
    @Nullable List<String> categories,
    int page,
    int size,
    @Nullable String sort) {
  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

  public enum TransactionType {
    INCOME,
    EXPENSE,
    ALL
  }
}
