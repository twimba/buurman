package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PaymentStatsResponse(
    int pendingCount,
    BigDecimal pendingAmount,
    int overdueCount,
    BigDecimal overdueAmount,
    String currency,
    List<MonthlyTrend> monthlyTrend
) {
    public record MonthlyTrend(String month, BigDecimal total) {}
}
