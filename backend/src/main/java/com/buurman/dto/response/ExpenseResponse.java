package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.buurman.domain.Expense;

public record ExpenseResponse(
    String identifier,
    PropertySummary property,
    Expense.ExpenseCategory category,
    BigDecimal amount,
    String currency,
    LocalDate expenseDate,
    String description,
    String notes,
    List<DocumentResponse> documents,
    Instant createdAt,
    Instant updatedAt) {}
