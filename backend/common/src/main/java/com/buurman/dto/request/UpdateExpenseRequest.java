package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Expense.ExpenseCategory;
import com.buurman.util.Generated;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Generated
public record UpdateExpenseRequest(
    Optional<ExpenseCategory> category,
    Optional<@Positive(message = "Amount must be positive") BigDecimal> amount,
    Optional<String> currency,
    Optional<LocalDate> expenseDate,
    Optional<
            @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters") String>
        description,
    Optional<String> notes) {}
