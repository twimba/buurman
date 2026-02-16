package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransactionResponse(
    String id,
    LocalDate date,
    TransactionType type,
    String description,
    PropertySummary property,
    String category,
    BigDecimal amount,
    String currency,
    List<DocumentResponse> documents) {
  public enum TransactionType {
    INCOME,
    EXPENSE
  }
}
