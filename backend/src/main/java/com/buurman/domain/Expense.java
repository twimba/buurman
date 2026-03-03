package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

  public enum ExpenseCategory {
    MAINTENANCE,
    REPAIR,
    UTILITY,
    TAX,
    INSURANCE,
    LEGAL,
    MARKETING,
    CLEANING,
    LANDSCAPING,
    PROPERTY_MANAGEMENT,
    FEES,
    PROPERTY_TAX,
    OTHER
  }

  private UUID id;
  @Builder.Default private Optional<Ulid> identifier = Optional.empty();
  private UUID teamId;
  private UUID propertyId;
  private ExpenseCategory category;
  private BigDecimal amount;
  private String currency;
  private LocalDate expenseDate;
  private String description;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
