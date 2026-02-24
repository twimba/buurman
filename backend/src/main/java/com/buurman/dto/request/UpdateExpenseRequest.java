package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.Expense.ExpenseCategory;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateExpenseRequest(
    Optional<ExpenseCategory> category,
    @Positive(message = "Amount must be positive") Optional<BigDecimal> amount,
    Optional<String> currency,
    Optional<LocalDate> expenseDate,
    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters") Optional<String> description,
    Optional<String> notes) {
  public UpdateExpenseRequest {
    category = Objects.requireNonNullElse(category, Optional.empty());
    amount = Objects.requireNonNullElse(amount, Optional.empty());
    currency = Objects.requireNonNullElse(currency, Optional.empty());
    expenseDate = Objects.requireNonNullElse(expenseDate, Optional.empty());
    description = Objects.requireNonNullElse(description, Optional.empty());
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
