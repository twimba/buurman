package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Expense;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Property expense record with category, amount, and supporting documents")
public record ExpenseResponse(
    @Schema(description = "Unique expense identifier", example = "exp_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
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
