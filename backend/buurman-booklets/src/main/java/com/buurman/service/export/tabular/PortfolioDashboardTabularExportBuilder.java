package com.buurman.service.export.tabular;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PortfolioDashboardResponse.PortfolioSummary;
import com.buurman.dto.response.PortfolioDashboardResponse.PropertyPerformance;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;

/** Builds the three-sheet TabularExport for the portfolio-wide dashboard. */
@Component
public class PortfolioDashboardTabularExportBuilder {

  public TabularExport build(PortfolioDashboardResponse dashboard) {
    PortfolioSummary s = dashboard.summary();
    TabularExport export = new TabularExport("portfolio-dashboard");

    TabularSheet summary =
        export.addSheet(
            "Portfolio Summary Metrics",
            List.of(
                TabularColumn.text("Metric"),
                TabularColumn.of("Value", TabularColumnFormat.NUMBER)));
    summary.addRow("Currency", dashboard.currency().orElse("N/A"));
    summary.addRow(
        "Properties with Financial Data",
        dashboard.propertiesWithFinancialData() + " / " + dashboard.totalProperties());
    addOptional(summary, "Total Portfolio Value", s.totalPortfolioValue());
    addOptional(summary, "Total Equity", s.totalEquity());
    addOptional(summary, "Monthly Cash Flow", s.monthlyCashFlow());
    addOptional(summary, "Annual NOI", s.annualNoi());
    addOptional(summary, "Weighted Cap Rate %", s.weightedCapRate());
    addOptional(summary, "Weighted Cash-on-Cash %", s.weightedCashOnCash());
    addOptional(summary, "Portfolio Occupancy %", s.portfolioOccupancy());
    addOptional(summary, "Debt-to-Equity", s.debtToEquity());
    addOptional(summary, "DSCR", s.portfolioDscr());
    addOptional(summary, "Income Concentration %", s.incomeConcentration());
    addOptional(summary, "Data Completeness %", s.dataCompleteness());

    TabularSheet cashFlow =
        export.addSheet(
            "Aggregated Monthly Cash Flow",
            List.of(
                TabularColumn.text("Month"),
                TabularColumn.of("Income", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Expenses", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Mortgage", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Net", TabularColumnFormat.CURRENCY)));
    for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
      cashFlow.addRow(m.month(), m.income(), m.expenses(), m.mortgage(), m.net());
    }

    TabularSheet comp =
        export.addSheet(
            "Property Comparison",
            List.of(
                TabularColumn.text("Property"),
                TabularColumn.text("Category"),
                TabularColumn.of("Monthly Cash Flow", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Annual NOI", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Cap Rate %", TabularColumnFormat.NUMBER),
                TabularColumn.of("Cash-on-Cash %", TabularColumnFormat.NUMBER),
                TabularColumn.of("Occupancy %", TabularColumnFormat.NUMBER),
                TabularColumn.of("Data Completeness %", TabularColumnFormat.NUMBER),
                TabularColumn.text("Currency"),
                TabularColumn.text("Currency Mismatch")));
    for (PropertyPerformance pp : dashboard.propertyComparison()) {
      comp.addRow(
          pp.address(),
          pp.category(),
          pp.monthlyCashFlow().isPresent() ? pp.monthlyCashFlow().get() : "N/A",
          pp.annualNoi().isPresent() ? pp.annualNoi().get() : "N/A",
          pp.capRate().isPresent() ? pp.capRate().get() : "N/A",
          pp.cashOnCash().isPresent() ? pp.cashOnCash().get() : "N/A",
          pp.occupancyRate().isPresent() ? pp.occupancyRate().get() : "N/A",
          pp.completenessPercent(),
          pp.currency().orElse("N/A"),
          pp.currencyMismatch());
    }
    return export;
  }

  private static void addOptional(TabularSheet sheet, String label, Optional<BigDecimal> value) {
    sheet.addRow(label, value.isPresent() ? value.get() : "N/A");
  }
}
