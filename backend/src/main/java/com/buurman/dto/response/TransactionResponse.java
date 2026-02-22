package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record TransactionResponse(
    String id,
    LocalDate date,
    TransactionType type,
    @Nullable String description,
    @Nullable PropertySummary property,
    @Nullable String category,
    BigDecimal amount,
    String currency,
    List<DocumentResponse> documents) {
  public enum TransactionType {
    INCOME,
    EXPENSE
  }
}
