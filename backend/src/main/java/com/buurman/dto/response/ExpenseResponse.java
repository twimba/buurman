package com.buurman.dto.response;

import com.buurman.domain.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        String identifier,
        UUID teamId,
        PropertySummary property,
        Expense.ExpenseCategory category,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String description,
        String notes,
        List<DocumentResponse> documents,
        Instant createdAt,
        Instant updatedAt
) {
}
