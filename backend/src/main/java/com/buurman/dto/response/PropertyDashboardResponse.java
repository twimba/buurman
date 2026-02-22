package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public record PropertyDashboardResponse(
    SummaryMetrics summary,
    CashFlowChartData cashFlow,
    EquityChartData equity,
    ExpenseBreakdownChartData expenseBreakdown,
    OccupancyChartData occupancy,
    DataCompleteness dataCompleteness,
    FutureTrendData futureTrend) {

  public record SummaryMetrics(
      @Nullable BigDecimal totalRoiPercent,
      @Nullable BigDecimal annualizedRoiPercent,
      @Nullable BigDecimal capRatePercent,
      @Nullable BigDecimal cashOnCashPercent,
      @Nullable BigDecimal monthlyCashFlow,
      @Nullable BigDecimal annualNoi,
      @Nullable BigDecimal totalEquity,
      @Nullable BigDecimal equityGrowthPercent,
      @Nullable BigDecimal occupancyRatePercent,
      @Nullable BigDecimal grossRentMultiplier,
      @Nullable String currency) {}

  public record CashFlowChartData(List<MonthlyDataPoint> months) {}

  public record MonthlyDataPoint(
      String month, BigDecimal income, BigDecimal expenses, BigDecimal mortgage, BigDecimal net) {}

  public record EquityChartData(
      @Nullable BigDecimal purchasePrice,
      @Nullable BigDecimal currentMarketValue,
      @Nullable BigDecimal mortgageBalance) {}

  public record ExpenseBreakdownChartData(
      List<CategorySlice> categories, List<ExpenseTimelineMonth> timeline) {}

  public record CategorySlice(String category, BigDecimal amount) {}

  public record ExpenseTimelineMonth(String month, Map<String, BigDecimal> categoryAmounts) {}

  public record OccupancyChartData(List<OccupancyDataPoint> months) {}

  public record OccupancyDataPoint(String month, BigDecimal occupancyPercent) {}

  public record DataCompleteness(
      boolean hasPurchasePrice,
      boolean hasMarketValue,
      boolean hasMortgageInfo,
      boolean hasOperatingCosts,
      boolean hasContracts,
      boolean hasPayments,
      boolean hasExpenses,
      int completenessPercent) {}

  public record FutureTrendData(List<FutureMonthDataPoint> months) {}

  public record FutureMonthDataPoint(
      String month,
      BigDecimal expectedIncome,
      BigDecimal expectedExpenses,
      BigDecimal expectedNet) {}
}
