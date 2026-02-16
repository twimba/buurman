package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseStatsResponse(
    BigDecimal totalAmount,
    String currency,
    List<CategoryTotal> topCategories,
    List<MonthlyTrend> monthlyTrend) {
  public record CategoryTotal(String category, BigDecimal total, int count) {}

  public record MonthlyTrend(String month, BigDecimal total) {}
}
