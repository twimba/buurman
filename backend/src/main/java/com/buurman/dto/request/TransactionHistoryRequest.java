package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
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

  // Bean Validation evaluates all constraints simultaneously, so @AssertTrue can run even when
  // @NotNull fails — null guards prevent NPE in that case.
  @AssertTrue(message = "End date must be after start date")
  @SuppressWarnings("ConstantConditions")
  public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

  public enum TransactionType {
    INCOME,
    EXPENSE,
    ALL
  }
}
