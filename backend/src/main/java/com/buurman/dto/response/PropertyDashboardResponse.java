package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PropertyDashboardResponse(
    SummaryMetrics summary,
    CashFlowChartData cashFlow,
    EquityChartData equity,
    ExpenseBreakdownChartData expenseBreakdown,
    OccupancyChartData occupancy,
    DataCompleteness dataCompleteness) {

  public record SummaryMetrics(
      BigDecimal totalRoiPercent,
      BigDecimal annualizedRoiPercent,
      BigDecimal capRatePercent,
      BigDecimal cashOnCashPercent,
      BigDecimal monthlyCashFlow,
      BigDecimal annualNoi,
      BigDecimal totalEquity,
      BigDecimal equityGrowthPercent,
      BigDecimal occupancyRatePercent,
      BigDecimal grossRentMultiplier,
      String currency) {}

  public record CashFlowChartData(List<MonthlyDataPoint> months) {}

  public record MonthlyDataPoint(
      String month, BigDecimal income, BigDecimal expenses, BigDecimal mortgage, BigDecimal net) {}

  public record EquityChartData(
      BigDecimal purchasePrice, BigDecimal currentMarketValue, BigDecimal mortgageBalance) {}

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
}
