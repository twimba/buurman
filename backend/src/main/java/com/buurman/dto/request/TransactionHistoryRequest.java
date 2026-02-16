package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record TransactionHistoryRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    TransactionType type,
    List<UUID> propertyIds,
    List<String> categories,
    int page,
    int size,
    String sort) {
  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

  public enum TransactionType {
    INCOME,
    EXPENSE,
    ALL
  }
}
