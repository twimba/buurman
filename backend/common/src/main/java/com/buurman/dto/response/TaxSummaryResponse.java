package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record TaxSummaryResponse(
    int year,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netIncome,
    List<CategoryExpenseSummary> expensesByCategory,
    List<PropertyFinancialSummary> properties,
    String currency) {}
