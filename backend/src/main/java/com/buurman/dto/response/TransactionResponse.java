package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record TransactionResponse(
    String id,
    LocalDate date,
    TransactionType type,
    Optional<String> description,
    Optional<PropertySummary> property,
    Optional<String> category,
    BigDecimal amount,
    String currency,
    List<DocumentResponse> documents) {
  public enum TransactionType {
    INCOME,
    EXPENSE
  }
}
