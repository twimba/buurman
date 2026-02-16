package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.buurman.domain.Expense;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateExpenseRequest(
    Expense.ExpenseCategory category,
    @Positive(message = "Amount must be positive") BigDecimal amount,
    String currency,
    LocalDate expenseDate,
    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters") String description,
    String notes) {}
