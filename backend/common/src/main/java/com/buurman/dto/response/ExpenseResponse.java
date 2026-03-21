package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Expense;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ExpenseResponse(
    Sid identifier,
    Optional<PropertySummary> property,
    Expense.ExpenseCategory category,
    BigDecimal amount,
    String currency,
    LocalDate expenseDate,
    Optional<String> description,
    Optional<String> notes,
    List<DocumentResponse> documents,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
