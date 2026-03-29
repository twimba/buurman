package com.buurman.dto.request;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record BulkCreateExpensesRequest(
    @NotNull(message = "Items are required") @Size(min = 1, max = 1000, message = "Between 1 and 1000 items allowed") List<CreateExpenseRequest> items) {}
