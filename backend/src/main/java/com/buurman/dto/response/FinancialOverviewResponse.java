package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialOverviewResponse(
        Period period,
        Income income,
        Expenses expenses,
        BigDecimal netProfit,
        String currency
) {
    public record Period(
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record Income(
            BigDecimal total,
            List<PropertyFinancialSummary> byProperty
    ) {}

    public record Expenses(
            BigDecimal total,
            List<CategoryExpenseSummary> byCategory,
            List<PropertyFinancialSummary> byProperty
    ) {}
}
