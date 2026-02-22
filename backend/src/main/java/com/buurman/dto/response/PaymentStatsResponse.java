package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record PaymentStatsResponse(
    int pendingCount,
    BigDecimal pendingAmount,
    int overdueCount,
    BigDecimal overdueAmount,
    @Nullable String currency,
    List<MonthlyTrend> monthlyTrend) {
  public record MonthlyTrend(String month, BigDecimal total) {}
}
