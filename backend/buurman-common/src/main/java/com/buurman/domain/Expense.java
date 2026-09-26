package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

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
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID propertyId;
  @Builder.Default private Optional<UUID> contactId = Optional.empty();
  // Empty means the expense is building-level and gets split across the property's units via
  // ExpenseAllocationService; present means it belongs to exactly one unit and gets no allocation
  // rows.
  @Builder.Default private Optional<UUID> unitId = Optional.empty();
  private ExpenseCategory category;
  private MoneyAmount amount;
  private LocalDate expenseDate;
  private String description;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
