package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
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
    MORTGAGE_PAYMENT,
    OTHER
  }

  private UUID id;
  private String identifier;
  private UUID teamId;
  private UUID propertyId;
  private ExpenseCategory category;
  private BigDecimal amount;
  private String currency;
  private LocalDate expenseDate;
  private String description;
  private @Nullable String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
