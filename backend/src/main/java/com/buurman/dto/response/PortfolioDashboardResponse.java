package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;

public record PortfolioDashboardResponse(
    PortfolioSummary summary,
    PropertyDashboardResponse.CashFlowChartData cashFlow,
    List<PropertyPerformance> propertyComparison,
    AllocationData allocation,
    PropertyDashboardResponse.OccupancyChartData occupancy,
    EquityCompositionData equityComposition,
    int propertiesWithFinancialData,
    int totalProperties,
    Optional<String> currency) {

  public record PortfolioSummary(
      Optional<BigDecimal> totalPortfolioValue,
      Optional<BigDecimal> totalEquity,
      Optional<BigDecimal> monthlyCashFlow,
      Optional<BigDecimal> annualNoi,
      Optional<BigDecimal> weightedCapRate,
      Optional<BigDecimal> weightedCashOnCash,
      Optional<BigDecimal> portfolioOccupancy,
      Optional<BigDecimal> debtToEquity,
      Optional<BigDecimal> portfolioDscr,
      Optional<BigDecimal> incomeConcentration,
      Optional<BigDecimal> dataCompleteness) {}

  public record PropertyPerformance(
      Sid identifier,
      String address,
      String category,
      Optional<BigDecimal> monthlyCashFlow,
      Optional<BigDecimal> annualNoi,
      Optional<BigDecimal> capRate,
      Optional<BigDecimal> cashOnCash,
      Optional<BigDecimal> occupancyRate,
      int completenessPercent,
      Optional<String> currency,
      boolean currencyMismatch) {}

  public record AllocationData(List<AllocationSlice> byCategory, List<AllocationSlice> byCountry) {}

  public record AllocationSlice(String label, BigDecimal value, BigDecimal percentage) {}

  public record EquityCompositionData(List<PropertyEquity> properties) {}

  public record PropertyEquity(
      Sid identifier,
      String address,
      Optional<BigDecimal> equity,
      Optional<BigDecimal> mortgage,
      Optional<BigDecimal> marketValue) {}
}
