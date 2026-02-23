package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record PaymentStatsResponse(
    int pendingCount,
    BigDecimal pendingAmount,
    int overdueCount,
    BigDecimal overdueAmount,
    Optional<String> currency,
    List<MonthlyTrend> monthlyTrend) {
  public record MonthlyTrend(String month, BigDecimal total) {}
}
