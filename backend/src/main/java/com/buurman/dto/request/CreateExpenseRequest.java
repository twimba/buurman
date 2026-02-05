package com.buurman.dto.request;

import com.buurman.domain.Expense;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull(message = "Property identifier is required")
        String propertyIdentifier,

        @NotNull(message = "Category is required")
        Expense.ExpenseCategory category,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String currency,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate,

        @NotNull(message = "Description is required")
        @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
        String description,

        String notes
) {
}
