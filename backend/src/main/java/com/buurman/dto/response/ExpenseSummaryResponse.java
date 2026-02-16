package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.buurman.domain.Expense;

public record ExpenseSummaryResponse(
    String period, List<CategoryTotal> byCategory, BigDecimal grandTotal, String currency) {
  public record CategoryTotal(Expense.ExpenseCategory category, BigDecimal total, int count) {}
}
