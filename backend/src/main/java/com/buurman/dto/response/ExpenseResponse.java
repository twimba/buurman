package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Expense;

public record ExpenseResponse(
    String identifier,
    @Nullable PropertySummary property,
    Expense.ExpenseCategory category,
    BigDecimal amount,
    String currency,
    LocalDate expenseDate,
    @Nullable String description,
    @Nullable String notes,
    List<DocumentResponse> documents,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
