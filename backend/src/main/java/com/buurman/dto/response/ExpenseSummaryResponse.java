package com.buurman.dto.response;

import com.buurman.domain.Expense;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSummaryResponse(
        String period,
        List<CategoryTotal> byCategory,
        BigDecimal grandTotal,
        String currency
) {
    public record CategoryTotal(
            Expense.ExpenseCategory category,
            BigDecimal total,
            int count
    ) {}
}
