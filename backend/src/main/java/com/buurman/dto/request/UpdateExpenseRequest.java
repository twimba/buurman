package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Expense.ExpenseCategory;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateExpenseRequest(
    @Nullable ExpenseCategory category,
    @Nullable @Positive(message = "Amount must be positive") BigDecimal amount,
    @Nullable String currency,
    @Nullable LocalDate expenseDate,
    @Nullable @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters") String description,
    @Nullable String notes) {}
